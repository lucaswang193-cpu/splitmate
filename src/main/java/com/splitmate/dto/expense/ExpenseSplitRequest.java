package com.splitmate.dto.expense;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ExpenseSplitRequest(

    @NotNull(message = "User id is required")
    UUID userId,

    @NotNull(message = "Share amount is required")
    @DecimalMin(value = "0.00", message = "Share amount must be greater than or equal to 0")
    @jakarta.validation.constraints.Digits(integer = 18, fraction = 2)
    BigDecimal shareAmount,

    @DecimalMin(value = "0.00", message = "Paid amount must be greater than or equal to 0")
    @jakarta.validation.constraints.Digits(integer = 18, fraction = 2)
    BigDecimal paidAmount
) {
}
