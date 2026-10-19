package com.splitmate.mapper;

import java.util.List;

import com.splitmate.dto.expense.ExpenseResponse;
import com.splitmate.dto.expense.ExpenseShareResponse;
import com.splitmate.entity.Expense;
import com.splitmate.entity.ExpenseCategory;
import com.splitmate.entity.ExpenseShare;

public class ExpenseMapper {

    private ExpenseMapper() {
    }

    public static ExpenseResponse toDto(Expense expense) {
        ExpenseCategory category = expense.getCategory();
        List<ExpenseShareResponse> shares = expense.getShares() == null
                ? List.of()
                : expense.getShares().stream()
                        .map(ExpenseMapper::toDto)
                        .toList();

        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getPayer().getId(),
                expense.getPayer().getName(),
                expense.getTitle(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                expense.getCurrency().getCode(),
                expense.getAmount(),
                expense.getExchangeRate(),
                expense.getBaseAmount(),
                expense.getExpenseAt(),
                expense.getNote(),
                shares,
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }

    public static ExpenseShareResponse toDto(ExpenseShare share) {
        return new ExpenseShareResponse(
                share.getId(),
                share.getExpense().getId(),
                share.getUser().getId(),
                share.getUser().getName(),
                share.getShareAmount(),
                share.getPaidAmount(),
                share.getStatus(),
                share.getCreatedAt(),
                share.getUpdatedAt()
        );
    }
}
