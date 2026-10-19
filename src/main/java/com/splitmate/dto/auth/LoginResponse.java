package com.splitmate.dto.auth;

import java.util.UUID;

import com.splitmate.entity.User;

public record LoginResponse(
    UUID userId,
    String name,
    String email,
    String phoneNumber,
    boolean phoneVerified,
    String avatarUrl,
    String accessToken
) {
    public static LoginResponse toDto(User user, String accessToken) {
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.isPhoneVerified(),
                user.getAvatarUrl(),
                accessToken
        );
    }
}
