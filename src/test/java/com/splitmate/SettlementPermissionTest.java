package com.splitmate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.entity.Currency;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.GroupMember;
import com.splitmate.entity.Settlement;
import com.splitmate.entity.User;
import com.splitmate.security.JwtTokenProvider;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private User payer;
    private User receiver;
    private User outsider;
    private ExpenseGroup group;

    @BeforeEach
    void setup() {
        owner = createUser("Owner");
        payer = createUser("Payer");
        receiver = createUser("Receiver");
        outsider = createUser("Outsider");

        Currency twd = entityManager.find(Currency.class, "TWD");

        group = ExpenseGroup.builder()
                .name("Settlement permission group")
                .owner(owner)
                .baseCurrency(twd)
                .inviteCode("ST" + UUID.randomUUID().toString().substring(0, 6))
                .build();
        entityManager.persist(group);

        persistMembership(owner, "OWNER");
        persistMembership(payer, "MEMBER");
        persistMembership(receiver, "MEMBER");

        entityManager.persist(Settlement.builder()
                .group(group)
                .fromUser(payer)
                .toUser(receiver)
                .amount(new BigDecimal("300.00"))
                .currency(twd)
                .build());

        entityManager.flush();
    }

    @Test
    void memberCanReadGroupSettlements() throws Exception {
        mockMvc.perform(get("/api/settlements/groups/{groupId}", group.getId())
                        .header("Authorization", bearer(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(300.00));
    }

    @Test
    void outsiderCannotReadGroupSettlements() throws Exception {
        mockMvc.perform(get("/api/settlements/groups/{groupId}", group.getId())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void memberCanCreateSettlementInOwnGroup() throws Exception {
        mockMvc.perform(post("/api/settlements")
                        .header("Authorization", bearer(receiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSettlementJson(receiver.getId(), payer.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fromUserId").value(receiver.getId().toString()))
                .andExpect(jsonPath("$.data.toUserId").value(payer.getId().toString()))
                .andExpect(jsonPath("$.data.amount").value(150.00));
    }

    @Test
    void outsiderCannotCreateSettlement() throws Exception {
        mockMvc.perform(post("/api/settlements")
                        .header("Authorization", bearer(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSettlementJson(payer.getId(), receiver.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void userCanOnlyQueryOwnSentOrReceivedSettlements() throws Exception {
        mockMvc.perform(get("/api/settlements")
                        .param("userId", payer.getId().toString())
                        .param("type", "sent")
                        .header("Authorization", bearer(payer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/settlements")
                        .param("userId", receiver.getId().toString())
                        .param("type", "received")
                        .header("Authorization", bearer(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/settlements")
                        .param("userId", payer.getId().toString())
                        .param("type", "sent")
                        .header("Authorization", bearer(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    private void persistMembership(User user, String role) {
        entityManager.persist(GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .build());
    }

    private User createUser(String name) {
        User user = User.builder()
                .name(name)
                .email(UUID.randomUUID() + "@example.com")
                .password("unused-test-password")
                .build();

        entityManager.persist(user);
        return user;
    }

    private String createSettlementJson(UUID fromUserId, UUID toUserId) {
        return """
                {
                  "groupId": "%s",
                  "fromUserId": "%s",
                  "toUserId": "%s",
                  "amount": 150.00,
                  "currency": "TWD"
                }
                """.formatted(group.getId(), fromUserId, toUserId);
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createToken(user);
    }
}