package com.splitmate.dto.balance;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
    UUID userId,
    String userName,
    BigDecimal totalShareAmount,
    BigDecimal totalPaidAmount,
    BigDecimal netAmount
) {
}
