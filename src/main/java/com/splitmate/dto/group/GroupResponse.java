package com.splitmate.dto.group;

import java.time.LocalDateTime;
import java.util.UUID;

import com.splitmate.entity.ExpenseGroup;

public record GroupResponse(
    UUID id,
    String name,
    String description,
    UUID ownerId,
    String ownerName,
    String baseCurrency,
    String inviteCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static GroupResponse toDto(ExpenseGroup group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getOwner().getId(),
                group.getOwner().getName(),
                group.getBaseCurrency().getCode(),
                group.getInviteCode(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
