package com.splitmate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.auth.ConfirmPhoneVerificationRequest;
import com.splitmate.dto.auth.CreatePhoneVerificationRequest;
import com.splitmate.dto.auth.GoogleLoginRequest;
import com.splitmate.dto.auth.LinkGoogleRequest;
import com.splitmate.dto.auth.LoginMethodsResponse;
import com.splitmate.dto.auth.LoginRequest;
import com.splitmate.dto.auth.LoginResponse;
import com.splitmate.dto.auth.PhoneVerificationResponse;
import com.splitmate.dto.auth.SetPasswordRequest;
import com.splitmate.dto.auth.UnlinkGoogleRequest;
import com.splitmate.dto.auth.UnlinkGoogleResponse;
import com.splitmate.dto.user.UserResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.security.AuthenticatedUser;
import com.splitmate.service.AuthService;
import com.splitmate.service.PhoneVerificationService;
import com.splitmate.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Transactional
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = authService.googleLogin(request);

        return ResponseEntity.ok(
                ApiResponse.success("Google login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UserResponse response = userService.getUserById(authenticatedUser.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/phone-verifications")
    public ResponseEntity<ApiResponse<PhoneVerificationResponse>> createPhoneVerification(
            @Valid @RequestBody CreatePhoneVerificationRequest request) {
        PhoneVerificationResponse response = phoneVerificationService.createPhoneVerification(request);
        return ResponseEntity.ok(ApiResponse.success("Phone verification code created", response));
    }

    @PostMapping("/phone-verifications/confirm")
    public ResponseEntity<ApiResponse<PhoneVerificationResponse>> confirmPhoneVerification(
            @Valid @RequestBody ConfirmPhoneVerificationRequest request) {
        PhoneVerificationResponse response = phoneVerificationService.confirmPhoneVerification(request);
        return ResponseEntity.ok(ApiResponse.success("Phone verified successfully", response));
    }

    @GetMapping("/login-methods")
    public ResponseEntity<ApiResponse<LoginMethodsResponse>> getLoginMethods() {
        return ResponseEntity.ok(
                ApiResponse.success(authService.getLoginMethods()));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>success("Password set successfully", null));
    }

    @PostMapping("/google/link")
    public ResponseEntity<ApiResponse<Void>> linkGoogle(
            @Valid @RequestBody LinkGoogleRequest request) {
        authService.linkGoogle(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>success("Google account linked successfully", null));
    }

    @PostMapping("/google/unlink")
    public ResponseEntity<ApiResponse<UnlinkGoogleResponse>> unlinkGoogle(
            @Valid @RequestBody UnlinkGoogleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Google account unlinked successfully",
                        authService.unlinkGoogle(request)));
    }
}
