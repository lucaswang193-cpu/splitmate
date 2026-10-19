package com.splitmate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.splitmate.entity.User;
import com.splitmate.security.JwtTokenProvider;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private User member;
    private User outsider;
    private ExpenseGroup group;

    @BeforeEach
    void setup() {
        owner = createUser("Owner");
        member = createUser("Member");
        outsider = createUser("Outsider");

        group = ExpenseGroup.builder()
                .name("Permission test group")
                .description("Invite preview must not expose this")
                .owner(owner)
                .baseCurrency(entityManager.find(Currency.class, "TWD"))
                .inviteCode("TEST" + UUID.randomUUID().toString().substring(0, 4))
                .build();

        entityManager.persist(group);
        entityManager.persist(GroupMember.builder()
                .group(group)
                .user(owner)
                .role("OWNER")
                .build());
        entityManager.persist(GroupMember.builder()
                .group(group)
                .user(member)
                .role("MEMBER")
                .build());
        entityManager.flush();
    }

    @Test
    void memberCanGetCompleteGroup() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}", group.getId())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(group.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Permission test group"))
                .andExpect(jsonPath("$.data.description")
                        .value("Invite preview must not expose this"))
                .andExpect(jsonPath("$.data.ownerId").value(owner.getId().toString()));
    }

    @Test
    void outsiderCannotGetCompleteGroup() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}", group.getId())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void authenticatedUserCanPreviewGroupByInviteCodeWithoutSensitiveData() throws Exception {
        mockMvc.perform(get("/api/groups/invite/{inviteCode}", group.getInviteCode())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Permission test group"))
                .andExpect(jsonPath("$.data.baseCurrency").value("TWD"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.ownerId").doesNotExist())
                .andExpect(jsonPath("$.data.ownerName").doesNotExist())
                .andExpect(jsonPath("$.data.inviteCode").doesNotExist());
    }

    @Test
    void anonymousUserCannotPreviewGroupByInviteCode() throws Exception {
        mockMvc.perform(get("/api/groups/invite/{inviteCode}", group.getInviteCode()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void memberCannotUpdateGroup() throws Exception {
        mockMvc.perform(put("/api/groups/{groupId}", group.getId())
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Changed by member",
                                  "description": "Should not be saved"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        entityManager.clear();

        ExpenseGroup unchanged = entityManager.find(ExpenseGroup.class, group.getId());
        assertEquals("Permission test group", unchanged.getName());
        assertEquals("Invite preview must not expose this", unchanged.getDescription());
    }

    @Test
    void memberCannotTransferGroupOwnership() throws Exception {
        mockMvc.perform(put("/api/groups/{groupId}/owner", group.getId())
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newOwnerId": "%s"
                                }
                                """.formatted(member.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        entityManager.clear();

        ExpenseGroup unchanged = entityManager.find(ExpenseGroup.class, group.getId());
        assertEquals(owner.getId(), unchanged.getOwner().getId());
    }

    @Test
    void memberCannotRemoveAnotherMember() throws Exception {
        mockMvc.perform(delete("/api/group-members/groups/{groupId}/users/{userId}",
                        group.getId(), owner.getId())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        long ownerMembershipCount = entityManager.createQuery("""
                select count(gm)
                from GroupMember gm
                where gm.group.id = :groupId
                  and gm.user.id = :userId
                """, Long.class)
                .setParameter("groupId", group.getId())
                .setParameter("userId", owner.getId())
                .getSingleResult();

        assertEquals(1L, ownerMembershipCount);
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

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createToken(user);
    }
}
