package com.splitmate.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseShareResponse(
    Long id,
    UUID expenseId,
    UUID userId,
    String userName,
    BigDecimal shareAmount,
    BigDecimal paidAmount,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
