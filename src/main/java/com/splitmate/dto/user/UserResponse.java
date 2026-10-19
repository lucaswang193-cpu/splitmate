package com.splitmate.dto.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.splitmate.entity.User;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String phoneNumber,
    boolean phoneVerified,
    LocalDateTime phoneVerifiedAt,
    String avatarUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UserResponse toDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.isPhoneVerified(),
                user.getPhoneVerifiedAt(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }   
}
