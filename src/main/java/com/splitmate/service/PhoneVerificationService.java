package com.splitmate.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.auth.ConfirmPhoneVerificationRequest;
import com.splitmate.dto.auth.CreatePhoneVerificationRequest;
import com.splitmate.dto.auth.PhoneVerificationFailureResponse;
import com.splitmate.dto.auth.PhoneVerificationResponse;
import com.splitmate.entity.PhoneVerification;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.PhoneVerificationFailedException;
import com.splitmate.repository.PhoneVerificationRepository;
import com.splitmate.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_EXPIRE_MINUTES = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VERIFICATION_RESEND_SECONDS = 60;

    private final UserRepository userRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final DataPermissionService dataPermissionService;

    @Transactional
    public PhoneVerificationResponse createPhoneVerification(CreatePhoneVerificationRequest request) {
        dataPermissionService.requireSelf(request.userId());
        User user = userRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new BusinessException("User not found"));
        LocalDateTime now = LocalDateTime.now();

        // Check if the user has requested a verification code too recently
        phoneVerificationRepository
                .findTopByUser_IdOrderByCreatedAtDescIdDesc(user.getId())
                .ifPresent(latest -> {
                    LocalDateTime nextAllowedAt = latest.getCreatedAt()
                            .plusSeconds(VERIFICATION_RESEND_SECONDS);

                    if (now.isBefore(nextAllowedAt)) {
                        throw new BusinessException(
                                "Please wait 60 seconds before requesting another code");
                    }
                });

        // Cancel any pending verifications for this user
        if (!request.phoneNumber().equals(user.getPhoneNumber())) {
            userRepository.findByPhoneNumber(request.phoneNumber())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new BusinessException("Phone number already exists");
                    });

            user.setPhoneNumber(request.phoneNumber());
            user.setPhoneVerified(false);
            user.setPhoneVerifiedAt(null);
        }

        // Cancel pending challenges across all phone numbers for this user.
        phoneVerificationRepository.findByUser_IdAndStatus(user.getId(), STATUS_PENDING)
                .forEach(pending -> pending.setStatus(STATUS_CANCELLED));

        PhoneVerification verification = PhoneVerification.builder()
                .user(user)
                .phoneNumber(request.phoneNumber())
                .code(generateVerificationCode())
                .expiresAt(now.plusMinutes(VERIFICATION_EXPIRE_MINUTES))
                .attemptCount(0)
                .status(STATUS_PENDING)
                .build();

        return PhoneVerificationResponse.toDto(phoneVerificationRepository.save(verification));
    }

    @Transactional(noRollbackFor = PhoneVerificationFailedException.class)
    public PhoneVerificationResponse confirmPhoneVerification(ConfirmPhoneVerificationRequest request) {
        dataPermissionService.requireSelf(request.userId());
        User user = userRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new BusinessException("User not found"));
        PhoneVerification verification = phoneVerificationRepository
                .findTopByUser_IdAndPhoneNumberAndStatusOrderByCreatedAtDesc(
                        request.userId(),
                        request.phoneNumber(),
                        STATUS_PENDING)
                .orElseThrow(() -> new BusinessException("Phone verification not found"));

        if (!verification.getPhoneNumber().equals(verification.getUser().getPhoneNumber())) {
            verification.setStatus(STATUS_CANCELLED);
            throw verificationFailure("Phone number changed; request a new verification code", verification);
        }

        if (!verification.getExpiresAt().isAfter(LocalDateTime.now())) {
            verification.setStatus(STATUS_EXPIRED);
            throw verificationFailure("Phone verification code expired", verification);
        }

        if (verification.getAttemptCount() >= MAX_VERIFY_ATTEMPTS) {
            verification.setStatus(STATUS_FAILED);
            throw verificationFailure("Too many verification attempts", verification);
        }

        verification.setAttemptCount(verification.getAttemptCount() + 1);

        if (!verification.getCode().equals(request.code())) {
            if (verification.getAttemptCount() >= MAX_VERIFY_ATTEMPTS) {
                verification.setStatus(STATUS_FAILED);
            }

            throw verificationFailure("Phone verification code incorrect", verification);
        }

        verification.setStatus(STATUS_VERIFIED);
        verification.setVerifiedAt(LocalDateTime.now());

        user.setPhoneNumber(verification.getPhoneNumber());
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(verification.getVerifiedAt());

        return PhoneVerificationResponse.toDto(verification);
    }

    private PhoneVerificationFailedException verificationFailure(String message, PhoneVerification verification) {
        int remaining = STATUS_PENDING.equals(verification.getStatus())
                ? Math.max(0, MAX_VERIFY_ATTEMPTS - verification.getAttemptCount())
                : 0;
        return new PhoneVerificationFailedException(message, new PhoneVerificationFailureResponse(
                verification.getAttemptCount(), remaining, verification.getStatus()));
    }

    private String generateVerificationCode() {
        StringBuilder builder = new StringBuilder(VERIFICATION_CODE_LENGTH);

        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            builder.append(RANDOM.nextInt(10));
        }

        return builder.toString();
    }
}
