package com.splitmate.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splitmate.entity.ExpenseShare;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    Page<ExpenseShare> findByExpense_Id(UUID expenseId, Pageable pageable);

    Page<ExpenseShare> findByUser_Id(UUID userId, Pageable pageable);

    Optional<ExpenseShare> findByExpense_IdAndUser_Id(UUID expenseId, UUID userId);

    @Modifying
    @Query("delete from ExpenseShare share where share.expense.id = :expenseId")
    void deleteAllByExpenseId(@Param("expenseId") UUID expenseId);

    @Query("""
        select coalesce(sum(s.paidAmount), 0)
             - coalesce(sum(s.shareAmount), 0)
        from ExpenseShare s
        where s.expense.group.id = :groupId
          and s.user.id = :userId
        """)
BigDecimal sumExpenseNetAmount(
        @Param("groupId") UUID groupId,
        @Param("userId") UUID userId
);
}
