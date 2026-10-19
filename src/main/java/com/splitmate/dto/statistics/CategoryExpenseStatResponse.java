package com.splitmate.dto.statistics;

import java.math.BigDecimal;

public record CategoryExpenseStatResponse(
    Long categoryId,
        String categoryName,
        long expenseCount,
        BigDecimal totalAmount,
        BigDecimal percentage
) {

}
