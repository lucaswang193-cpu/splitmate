package com.splitmate.dto.user;

import com.splitmate.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email must be less than 255 characters")
    String email,

    @Size(max = 30, message = "Phone number must be less than 30 characters")
    String phoneNumber,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    String password,

    @Size(max = 255, message = "Avatar URL must be less than 255 characters")
    String avatarUrl) {
        public User toEntity() {
        return User.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .password(password)
                .avatarUrl(avatarUrl)
                .build();
    }

}
