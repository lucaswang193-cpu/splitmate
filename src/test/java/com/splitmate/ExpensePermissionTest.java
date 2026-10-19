package com.splitmate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import com.splitmate.entity.Expense;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.ExpenseShare;
import com.splitmate.entity.GroupMember;
import com.splitmate.entity.User;
import com.splitmate.security.JwtTokenProvider;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpensePermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private User payer;
    private User member;
    private User outsider;
    private ExpenseGroup group;
    private Expense expense;

    @BeforeEach
    void setup() {
        owner = createUser("Owner");
        payer = createUser("Payer");
        member = createUser("Member");
        outsider = createUser("Outsider");

        group = ExpenseGroup.builder()
                .name("Expense permission group")
                .owner(owner)
                .baseCurrency(entityManager.find(Currency.class, "TWD"))
                .inviteCode("EX" + UUID.randomUUID().toString().substring(0, 6))
                .build();
        entityManager.persist(group);

        persistMembership(owner, "OWNER");
        persistMembership(payer, "MEMBER");
        persistMembership(member, "MEMBER");

        expense = Expense.builder()
                .id(UUID.randomUUID())
                .group(group)
                .payer(payer)
                .title("Original expense")
                .currency(entityManager.find(Currency.class, "TWD"))
                .amount(new BigDecimal("100.00"))
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("100.00"))
                .expenseAt(LocalDateTime.of(2026, 10, 14, 12, 0))
                .note("Original note")
                .build();
        entityManager.persist(expense);

        ExpenseShare payerShare = ExpenseShare.builder()
                .expense(expense)
                .user(payer)
                .shareAmount(new BigDecimal("50.00"))
                .paidAmount(new BigDecimal("100.00"))
                .status("PAID")
                .build();
        ExpenseShare memberShare = ExpenseShare.builder()
                .expense(expense)
                .user(member)
                .shareAmount(new BigDecimal("50.00"))
                .paidAmount(BigDecimal.ZERO)
                .status("OWING")
                .build();
        entityManager.persist(payerShare);
        entityManager.persist(memberShare);
        expense.setShares(List.of(payerShare, memberShare));
        entityManager.flush();
    }

    @Test
    void memberCanReadExpenseData() throws Exception {
        mockMvc.perform(get("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(expense.getId().toString()))
                .andExpect(jsonPath("$.data.title").value("Original expense"));

        mockMvc.perform(get("/api/expenses")
                        .param("groupId", group.getId().toString())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/expenses/groups/{groupId}/balances", group.getId())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/expense-shares/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void outsiderCannotReadExpenseData() throws Exception {
        mockMvc.perform(get("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses")
                        .param("groupId", group.getId().toString())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses/groups/{groupId}/balances", group.getId())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expense-shares/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void outsiderCannotCreateExpense() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(UUID.randomUUID(), "Outsider expense")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void memberCannotUpdateOrDeleteAnotherMembersExpense() throws Exception {
        mockMvc.perform(put("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson("Changed by member")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        entityManager.clear();
        Expense unchanged = entityManager.find(Expense.class, expense.getId());
        assertNotNull(unchanged);
        assertEquals("Original expense", unchanged.getTitle());

        mockMvc.perform(delete("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        entityManager.clear();
        assertNotNull(entityManager.find(Expense.class, expense.getId()));
    }

    @Test
    void payerCanUpdateOwnExpense() throws Exception {
        mockMvc.perform(put("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(payer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson("Updated by payer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated by payer"));

        entityManager.flush();
        entityManager.clear();
        assertEquals("Updated by payer", entityManager.find(Expense.class, expense.getId()).getTitle());
    }

    @Test
    void ownerCanUpdateAndDeleteAnotherMembersExpense() throws Exception {
        mockMvc.perform(put("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson("Updated by owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated by owner"));

        mockMvc.perform(delete("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/expenses/{expenseId}", expense.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Expense not found"));
    }

    @Test
    void userCannotQueryAnotherPayersExpenses() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .param("payerId", payer.getId().toString())
                        .header("Authorization", bearer(member)))
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

    private String createRequestJson(UUID expenseId, String title) {
        return """
                {
                  "id": "%s",
                  "groupId": "%s",
                  "payerId": "%s",
                  "title": "%s",
                  "categoryId": null,
                  "currency": "TWD",
                  "amount": 100.00,
                  "exchangeRate": 1.000000,
                  "baseAmount": 100.00,
                  "expenseAt": "2026-10-14T12:00:00",
                  "note": "Permission test",
                  "splits": [
                    { "userId": "%s", "shareAmount": 50.00, "paidAmount": 100.00 },
                    { "userId": "%s", "shareAmount": 50.00, "paidAmount": 0.00 }
                  ]
                }
                """.formatted(expenseId, group.getId(), payer.getId(), title, payer.getId(), member.getId());
    }

    private String updateRequestJson(String title) {
        return """
                {
                  "payerId": "%s",
                  "title": "%s",
                  "categoryId": null,
                  "currency": "TWD",
                  "amount": 100.00,
                  "exchangeRate": 1.000000,
                  "baseAmount": 100.00,
                  "expenseAt": "2026-10-14T12:00:00",
                  "note": "Permission test",
                  "splits": [
                    { "userId": "%s", "shareAmount": 50.00, "paidAmount": 100.00 },
                    { "userId": "%s", "shareAmount": 50.00, "paidAmount": 0.00 }
                  ]
                }
                """.formatted(payer.getId(), title, payer.getId(), member.getId());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createToken(user);
    }
}
