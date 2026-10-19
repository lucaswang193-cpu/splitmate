package com.splitmate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.UserIdentity;
import com.splitmate.enums.AuthProvider;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    // 依登入平台和平台帳號 ID，找到對應的帳號關聯。
    Optional<UserIdentity> findByProviderAndProviderUserId(
            AuthProvider provider,
            String providerUserId
    );

    // 檢查使用者是否已綁定該平台。
    boolean existsByUser_IdAndProvider(
            UUID userId,
            AuthProvider provider
    );
    // 依使用者 ID 和登入平台，找到對應的帳號關聯。
    Optional<UserIdentity> findByUser_IdAndProvider(
        UUID userId,
        AuthProvider provider
);

}
