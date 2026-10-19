package com.splitmate.dto.group;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TransferGroupOwnerRequest(
    @NotNull(message = "New owner id is required")
    UUID newOwnerId
) {
}
