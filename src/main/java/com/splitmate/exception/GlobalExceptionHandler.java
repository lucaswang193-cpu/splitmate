package com.splitmate.exception;

import java.util.stream.Collectors;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.splitmate.response.ApiResponse;
import com.splitmate.dto.auth.PhoneVerificationFailureResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(PhoneVerificationFailedException.class)
        public ResponseEntity<ApiResponse<PhoneVerificationFailureResponse>> handlePhoneVerificationFailure(
                        PhoneVerificationFailedException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.fail(
                                "PHONE_VERIFICATION_FAILED", ex.getMessage(), ex.getData()));
        }

        @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest(
                        org.springframework.http.converter.HttpMessageNotReadableException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.fail(
                                "BAD_REQUEST", "Invalid request JSON or field format"));
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.fail("EXPENSE_ID_CONFLICT", ex.getMessage()));
        }

        // Handle BusinessException and return a structured error response
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
                return ResponseEntity
                                .badRequest()
                                .body(ApiResponse.fail("BUSINESS_ERROR", ex.getMessage()));
        }

        // Handle validation errors and return a structured error response
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                return ResponseEntity
                                .badRequest()
                                .body(ApiResponse.fail("VALIDATION_ERROR", message));
        }

        // Handle IllegalArgumentException and return a structured error response
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
                return ResponseEntity
                                .badRequest()
                                .body(ApiResponse.fail("BAD_REQUEST", ex.getMessage()));
        }

        // Handle all other exceptions and return a generic error response
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.fail("INTERNAL_SERVER_ERROR", "Unexpected server error"));
        }

        // Handle missing or invalid query parameters and return a structured error
        // status code 400
        @ExceptionHandler({
                        org.springframework.web.bind.MissingServletRequestParameterException.class,
                        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
        })
        public ResponseEntity<ApiResponse<Void>> handleQueryParameterException(
                        Exception ex) {
                return ResponseEntity.badRequest()
                                .body(ApiResponse.fail(
                                                "INVALID_QUERY_PARAMETER",
                                                "Required query parameter is missing or has an invalid format"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
                        AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.fail("FORBIDDEN", "Access denied"));
        }

        // Handle login rate limiting and return a structured error response
        //status code 429
        @ExceptionHandler(LoginRateLimitedException.class)
        public ResponseEntity<ApiResponse<Void>> handleLoginRateLimited(
                        LoginRateLimitedException ex) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ApiResponse.fail(
                                                "LOGIN_RATE_LIMITED", ex.getMessage()));
        }
        // Handle Redis connection failures and return a structured error response
        //status code 503
        @ExceptionHandler(RedisConnectionFailureException.class)
        public ResponseEntity<ApiResponse<Void>> handleRedisConnectionFailure(
                        RedisConnectionFailureException ex) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(ApiResponse.fail(
                                                "SERVICE_UNAVAILABLE",
                                                "Login is temporarily unavailable. Please try again later"));
        }
}
