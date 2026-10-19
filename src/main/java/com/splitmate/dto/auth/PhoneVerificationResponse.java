package com.splitmate.dto.auth;

import java.time.LocalDateTime;

import com.splitmate.entity.PhoneVerification;

public record PhoneVerificationResponse(
    Long id,
    String phoneNumber,
    
    LocalDateTime expiresAt,
    LocalDateTime verifiedAt,
    int attemptCount,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PhoneVerificationResponse toDto(PhoneVerification verification) {
        return new PhoneVerificationResponse(
                verification.getId(),
                verification.getPhoneNumber(),
                
                verification.getExpiresAt(),
                verification.getVerifiedAt(),
                verification.getAttemptCount(),
                verification.getStatus(),
                verification.getCreatedAt(),
                verification.getUpdatedAt()
        );
    }
}
