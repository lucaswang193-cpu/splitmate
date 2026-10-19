package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.splitmate.dto.statistics.CategoryExpenseStatResponse;
import com.splitmate.dto.statistics.GroupExpenseStatsResponse;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.ExpenseRepository;
import com.splitmate.repository.projection.CategoryExpenseSummary;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseStatisticsService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseRepository expenseRepository;
    private final ExpenseGroupService expenseGroupService;
    private final DataPermissionService dataPermissionService;
    @Transactional(readOnly = true)
    public GroupExpenseStatsResponse getCategoryStats(
            UUID groupId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (from == null || to == null) {
            throw new BusinessException("Start and end times are required");
        }

        if (!from.isBefore(to)) {
            throw new BusinessException("Start time must be before end time");
        }

        dataPermissionService.requireGroupMember(groupId);

        ExpenseGroup group =
                expenseGroupService.getGroupEntityById(groupId);

        List<CategoryExpenseSummary> summaries =
                expenseRepository.summarizeByCategory(groupId, from, to);

        BigDecimal totalAmount = summaries.stream()
                .map(CategoryExpenseSummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long expenseCount = summaries.stream()
                .mapToLong(summary -> summary.getExpenseCount())
                .sum();

        List<CategoryExpenseStatResponse> categories = summaries.stream()
                .map(summary -> new CategoryExpenseStatResponse(
                        summary.getCategoryId(),
                        summary.getCategoryId() == null
                                ? "未分類"
                                : summary.getCategoryName(),
                        summary.getExpenseCount(),
                        summary.getTotalAmount(),
                        calculatePercentage(summary.getTotalAmount(), totalAmount)
                ))
                .toList();

        return new GroupExpenseStatsResponse(
                groupId,
                group.getBaseCurrency().getCode(),
                from,
                to,
                expenseCount,
                totalAmount,
                categories
        );
    }

    private BigDecimal calculatePercentage(
            BigDecimal categoryAmount,
            BigDecimal totalAmount
    ) {
        if (totalAmount.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return categoryAmount
                .multiply(HUNDRED)
                .divide(totalAmount, 2, RoundingMode.HALF_UP);
    }
}
