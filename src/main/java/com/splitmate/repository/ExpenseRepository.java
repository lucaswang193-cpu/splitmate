package com.splitmate.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splitmate.entity.Expense;
import com.splitmate.repository.projection.CategoryExpenseSummary;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Page<Expense> findByGroup_Id(UUID groupId, Pageable pageable);

    Page<Expense> findByPayer_Id(UUID payerId, Pageable pageable);

    // 使用 LEFT JOIN，讓沒有分類的支出也納入統計
    @Query("""
            select c.id as categoryId,
                   c.name as categoryName,
                   count(e.id) as expenseCount,
                   sum(e.baseAmount) as totalAmount
            from Expense e
            left join e.category c
            where e.group.id = :groupId
              and e.expenseAt >= :from
              and e.expenseAt < :to
            group by c.id, c.name
            order by sum(e.baseAmount) desc, c.id asc
            """)
    List<CategoryExpenseSummary> summarizeByCategory(
            @Param("groupId") UUID groupId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
