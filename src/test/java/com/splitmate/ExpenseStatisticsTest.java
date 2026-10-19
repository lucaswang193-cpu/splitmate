package com.splitmate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.entity.Currency;
import com.splitmate.entity.Expense;
import com.splitmate.entity.ExpenseCategory;
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
class ExpenseStatisticsTest {
    private static final String FROM = "2026-09-01T00:00:00";
    private static final String TO = "2026-10-01T00:00:00";

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private JwtTokenProvider tokens;

    private User owner;
    private ExpenseGroup group;
    private ExpenseCategory category;
    private String token;

    @BeforeEach
    void setup() {
        owner = newUser();
        group = newGroup();
        em.persist(GroupMember.builder().group(group).user(owner).role("OWNER").build());
        category = ExpenseCategory.builder().name("Stats " + UUID.randomUUID()).build();
        em.persist(category);
        em.flush();
        token = tokens.createToken(owner);
    }

    @Test
    void aggregatesBaseAmountsIncludesUncategorizedAndRespectsBoundaries() throws Exception {
        addExpense(group, category, FROM, "100");
        addExpense(group, category, "2026-09-15T12:30:00", "200");
        addExpense(group, null, "2026-09-30T23:59:59.999999", "100");
        addExpense(group, category, "2026-08-31T23:59:59.999999", "900");
        addExpense(group, category, TO, "900");
        addExpense(newGroup(), category, FROM, "900");
        em.persist(Settlement.builder().group(group).fromUser(owner).toUser(newUser())
                .currency(em.find(Currency.class, "TWD")).amount(new BigDecimal("900")).build());
        em.flush();
        em.clear();

        mvc.perform(query().param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseCurrency").value("TWD"))
                .andExpect(jsonPath("$.data.expenseCount").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(400))
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.categories[0].totalAmount").value(300))
                .andExpect(jsonPath("$.data.categories[0].expenseCount").value(2))
                .andExpect(jsonPath("$.data.categories[0].percentage").value(75))
                .andExpect(jsonPath("$.data.categories[1].categoryId").isEmpty())
                .andExpect(jsonPath("$.data.categories[1].categoryName").value("\u672a\u5206\u985e"))
                .andExpect(jsonPath("$.data.categories[1].percentage").value(25));
    }

    @Test
    void emptyRangeReturnsZeroAndEmptyList() throws Exception {
        mvc.perform(query().param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expenseCount").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0))
                .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    @Test
    void rejectsEqualAndReversedRanges() throws Exception {
        mvc.perform(query().param("from", FROM).param("to", FROM))
                .andExpect(status().isBadRequest());
        mvc.perform(query().param("from", TO).param("to", FROM))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingAndMalformedParameters() throws Exception {
        mvc.perform(query().param("from", FROM)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_QUERY_PARAMETER"));
        mvc.perform(query().param("from", "not-a-date").param("to", TO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_QUERY_PARAMETER"));
    }

    @Test
    void rejectsAnonymousRequest() throws Exception {
        mvc.perform(get(path()).param("from", FROM).param("to", TO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonMemberWithoutReturningStatistics() throws Exception {
        User outsider = newUser();
        em.flush();
        token = tokens.createToken(outsider);
        mvc.perform(query().param("from", FROM).param("to", TO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private String path() {
        return "/api/expenses/groups/" + group.getId() + "/category-stats";
    }

    private MockHttpServletRequestBuilder query() {
        return get(path()).header("Authorization", "Bearer " + token);
    }

    private User newUser() {
        User user = User.builder().name("Stats user").email(UUID.randomUUID() + "@example.com")
                .password("unused-test-password").build();
        em.persist(user);
        return user;
    }

    private ExpenseGroup newGroup() {
        ExpenseGroup value = ExpenseGroup.builder().name("Stats group").owner(owner)
                .baseCurrency(em.find(Currency.class, "TWD"))
                .inviteCode(UUID.randomUUID().toString().substring(0, 8)).build();
        em.persist(value);
        return value;
    }

    private void addExpense(ExpenseGroup target, ExpenseCategory category, String at, String base) {
        BigDecimal baseAmount = new BigDecimal(base);
        em.persist(Expense.builder().id(UUID.randomUUID()).group(target).payer(owner)
                .title("Statistics fixture").category(category).currency(em.find(Currency.class, "JPY"))
                .amount(baseAmount.multiply(BigDecimal.TEN)).exchangeRate(new BigDecimal("0.1"))
                .baseAmount(baseAmount).expenseAt(LocalDateTime.parse(at)).build());
    }
}
