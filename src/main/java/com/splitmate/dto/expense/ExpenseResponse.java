package com.splitmate.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    UUID groupId,
    UUID payerId,
    String payerName,
    String title,
    Long categoryId,
    String categoryName,
    String currency,
    BigDecimal amount,
    BigDecimal exchangeRate,
    BigDecimal baseAmount,
    LocalDateTime expenseAt,
    String note,
    List<ExpenseShareResponse> shares,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
