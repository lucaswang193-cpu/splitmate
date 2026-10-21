package com.splitmate.service;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.splitmate.dto.auth.GoogleLoginRequest;
import com.splitmate.dto.auth.LinkGoogleRequest;
import com.splitmate.dto.auth.LoginMethodsResponse;
import com.splitmate.dto.auth.LoginRequest;
import com.splitmate.dto.auth.LoginResponse;
import com.splitmate.dto.auth.SetPasswordRequest;
import com.splitmate.dto.auth.UnlinkGoogleRequest;
import com.splitmate.dto.auth.UnlinkGoogleResponse;
import com.splitmate.entity.User;
import com.splitmate.entity.UserIdentity;
import com.splitmate.enums.AuthProvider;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.LoginRateLimitedException;
import com.splitmate.repository.UserIdentityRepository;
import com.splitmate.repository.UserRepository;
import com.splitmate.security.JwtTokenProvider;
import com.splitmate.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;

        private final GoogleTokenService googleTokenService;
        private final UserIdentityRepository userIdentityRepository;

        private final LoginAttemptService loginAttemptService;

        @Transactional(readOnly = true)
        public LoginResponse login(LoginRequest request) {
                String email = request.email();

                if (loginAttemptService.isBlocked(email)) {
                        throw new LoginRateLimitedException();
                }

                User user = userRepository.findByEmail(email).orElse(null);

                boolean passwordValid = user != null
                                && user.getPassword() != null
                                && !user.getPassword().isBlank()
                                && passwordEncoder.matches(
                                                request.password(), user.getPassword());

                if (!passwordValid) {
                        loginAttemptService.recordFailure(email);
                        throw new BusinessException("Email or password incorrect");
                }

                String accessToken = jwtTokenProvider.createToken(user);
                loginAttemptService.clearFailures(email);

                return LoginResponse.toDto(user, accessToken);
        }

        // google login
        public LoginResponse googleLogin(GoogleLoginRequest request) {
                GoogleIdToken.Payload payload = googleTokenService.verify(request.credential());

                UserIdentity identity = userIdentityRepository
                                .findByProviderAndProviderUserId(
                                                AuthProvider.GOOGLE, payload.getSubject())
                                .orElse(null);

                User user;

                if (identity != null) {
                        user = identity.getUser();
                } else {
                        // 相同 Email 的既有帳號，必須先登入本人帳號再綁定。
                        if (userRepository.existsByEmail(payload.getEmail())) {
                                throw new BusinessException(
                                                "Please sign in to your existing account and link Google");
                        }

                        String name = (String) payload.get("name");
                        if (name == null || name.isBlank()) {
                                name = "Google User";
                        }

                        String avatarUrl = (String) payload.get("picture");

                        user = userRepository.save(User.builder()
                                        .name(name.substring(0, Math.min(name.length(), 100)))
                                        .email(payload.getEmail())
                                        .avatarUrl(avatarUrl != null && avatarUrl.length() <= 255
                                                        ? avatarUrl
                                                        : null)
                                        .password(null)
                                        .phoneVerified(false)
                                        .build());

                        userIdentityRepository.save(UserIdentity.builder()
                                        .user(user)
                                        .provider(AuthProvider.GOOGLE)
                                        .providerUserId(payload.getSubject())
                                        .build());
                }

                String accessToken = jwtTokenProvider.createToken(user);
                return LoginResponse.toDto(user, accessToken);
        }

        @Transactional(readOnly = true)
        public LoginMethodsResponse getLoginMethods() {
                // 直接從登入身分取得 ID，只能查詢本人。
                User user = userRepository.findById(SecurityUtils.getCurrentUser().id())
                                .orElseThrow(() -> new BusinessException("User not found"));

                boolean passwordSet = user.getPassword() != null
                                && !user.getPassword().isBlank();

                boolean googleLinked = userIdentityRepository.existsByUser_IdAndProvider(
                                user.getId(),
                                AuthProvider.GOOGLE);

                return new LoginMethodsResponse(passwordSet, googleLinked);
        }

        @Transactional
        public void setPassword(SetPasswordRequest request) {
                GoogleIdToken.Payload payload = googleTokenService.verify(request.credential());

                UserIdentity identity = userIdentityRepository
                                .findByProviderAndProviderUserId(
                                                AuthProvider.GOOGLE, payload.getSubject())
                                .orElseThrow(() -> new BusinessException("Google account not linked"));

                var currentUserId = SecurityUtils.getCurrentUser().id();

                if (!identity.getUser().getId().equals(currentUserId)) {
                        throw new BusinessException("Google account does not match current user");
                }

                // 鎖定使用者，避免同時設定密碼互相覆蓋。
                User user = userRepository.findByIdForUpdate(currentUserId)
                                .orElseThrow(() -> new BusinessException("User not found"));

                if (user.getPassword() != null && !user.getPassword().isBlank()) {
                        throw new BusinessException("Password already set");
                }

                // BCrypt 的上限以 UTF-8 位元組計算。
                if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
                        throw new BusinessException("Password must not exceed 72 UTF-8 bytes");
                }

                user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        @Transactional
        public void linkGoogle(LinkGoogleRequest request) {
                GoogleIdToken.Payload payload = googleTokenService.verify(request.credential());

                User user = userRepository
                                .findByIdForUpdate(SecurityUtils.getCurrentUser().id())
                                .orElseThrow(() -> new BusinessException("User not found"));

                if (user.getPassword() == null
                                || user.getPassword().isBlank()
                                || !passwordEncoder.matches(
                                                request.currentPassword(), user.getPassword())) {
                        throw new BusinessException("Password incorrect");
                }

                UserIdentity existingIdentity = userIdentityRepository
                                .findByProviderAndProviderUserId(
                                                AuthProvider.GOOGLE, payload.getSubject())
                                .orElse(null);

                if (existingIdentity != null) {
                        if (!existingIdentity.getUser().getId().equals(user.getId())) {
                                throw new BusinessException(
                                                "Google account is already linked to another user");
                        }
                        return;
                }

                if (userIdentityRepository.existsByUser_IdAndProvider(
                                user.getId(), AuthProvider.GOOGLE)) {
                        throw new BusinessException("A Google account is already linked");
                }

                userIdentityRepository.save(UserIdentity.builder()
                                .user(user)
                                .provider(AuthProvider.GOOGLE)
                                .providerUserId(payload.getSubject())
                                .build());
        }

        @Transactional
        public UnlinkGoogleResponse unlinkGoogle(UnlinkGoogleRequest request) {
                User user = userRepository
                                .findByIdForUpdate(SecurityUtils.getCurrentUser().id())
                                .orElseThrow(() -> new BusinessException("User not found"));

                if (user.getPassword() == null || user.getPassword().isBlank()) {
                        throw new BusinessException("Please set a password before unlinking Google");
                }

                if (!passwordEncoder.matches(
                                request.currentPassword(), user.getPassword())) {
                        throw new BusinessException("Password incorrect");
                }

                UserIdentity identity = userIdentityRepository
                                .findByUser_IdAndProvider(user.getId(), AuthProvider.GOOGLE)
                                .orElseThrow(() -> new BusinessException("Google account not linked"));

                String googleSubject = identity.getProviderUserId();
                userIdentityRepository.delete(identity);

                return new UnlinkGoogleResponse(googleSubject);
        }

}
