package com.splitmate.dto.auth;

public record PhoneVerificationFailureResponse(
        int attemptCount,
        int remainingAttempts,
        String status
) {}
