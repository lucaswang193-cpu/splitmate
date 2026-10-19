package com.splitmate.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    String name,

    @Size(max = 30, message = "Phone number must be less than 30 characters")
    String phoneNumber,

    @Size(max = 255, message = "Avatar URL must be less than 255 characters")
    String avatarUrl
) {
}
