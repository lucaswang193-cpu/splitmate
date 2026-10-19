package com.splitmate.dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePhoneVerificationRequest(
    @NotNull(message = "User id is required")
    UUID userId,

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must be less than 30 characters")
    String phoneNumber
) {
}
