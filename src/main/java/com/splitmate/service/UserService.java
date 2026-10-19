package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.user.CreateUserRequest;
import com.splitmate.dto.user.UpdateUserRequest;
import com.splitmate.dto.user.UserResponse;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.UserRepository;
import com.splitmate.security.SecurityUtils;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataPermissionService dataPermissionService;
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }

        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BusinessException("Phone number already exists");
        }

        User user = request.toEntity();
        user.setPassword(passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        return UserResponse.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        dataPermissionService.requireSelf(userId);

        return UserResponse.toDto(getUserEntityById(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        // 只讀取本人資料，避免透過 Email 查詢其他使用者。
        User currentUser = getUserEntityById(
                SecurityUtils.getCurrentUser().id());

        if (!currentUser.getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Access denied");
        }

        return UserResponse.toDto(currentUser);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {

        dataPermissionService.requireSelf(userId);

        User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException("User not found"));
        validatePhoneNumberForUpdate(user, request.phoneNumber());

        // 手機號碼改變後，新號碼必須重新驗證。
        if (!Objects.equals(user.getPhoneNumber(), request.phoneNumber())) {
            user.setPhoneVerified(false);
            user.setPhoneVerifiedAt(null);
        }
        user.setName(request.name());
        user.setPhoneNumber(request.phoneNumber());
        user.setAvatarUrl(request.avatarUrl());

        return UserResponse.toDto(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private void validatePhoneNumberForUpdate(User user, String phoneNumber) {
        if (phoneNumber == null) {
            return;
        }

        userRepository.findByPhoneNumber(phoneNumber)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new BusinessException("Phone number already exists");
                });
    }
}
