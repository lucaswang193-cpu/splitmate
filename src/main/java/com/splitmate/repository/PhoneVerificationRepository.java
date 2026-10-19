package com.splitmate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.PhoneVerification;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    List<PhoneVerification> findByUser_IdAndStatus(UUID userId, String status);

    Optional<PhoneVerification> findTopByUser_IdAndPhoneNumberAndStatusOrderByCreatedAtDesc(
            UUID userId,
            String phoneNumber,
            String status
    );
    Optional<PhoneVerification> findTopByUser_IdOrderByCreatedAtDescIdDesc(
        UUID userId
);
}
