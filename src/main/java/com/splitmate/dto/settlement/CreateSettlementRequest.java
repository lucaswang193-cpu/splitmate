package com.splitmate.dto.settlement;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSettlementRequest(

    @NotNull(message = "Group id is required")
    UUID groupId,

    @NotNull(message = "From user id is required")
    UUID fromUserId,

    @NotNull(message = "To user id is required")
    UUID toUserId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency
) {
}
