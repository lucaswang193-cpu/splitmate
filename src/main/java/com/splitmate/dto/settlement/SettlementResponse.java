package com.splitmate.dto.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.splitmate.entity.Settlement;

public record SettlementResponse(
    Long id,
    UUID groupId,
    UUID fromUserId,
    String fromUserName,
    UUID toUserId,
    String toUserName,
    BigDecimal amount,
    String currency,
    LocalDateTime settledAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SettlementResponse toDto(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getGroup().getId(),
                settlement.getFromUser().getId(),
                settlement.getFromUser().getName(),
                settlement.getToUser().getId(),
                settlement.getToUser().getName(),
                settlement.getAmount(),
                settlement.getCurrency().getCode(),
                settlement.getSettledAt(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }
}
