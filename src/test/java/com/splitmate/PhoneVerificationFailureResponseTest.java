package com.splitmate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.splitmate.dto.auth.PhoneVerificationFailureResponse;
import com.splitmate.exception.GlobalExceptionHandler;
import com.splitmate.exception.PhoneVerificationFailedException;
import com.splitmate.response.ApiResponse;

class PhoneVerificationFailureResponseTest {
    @Test
    void returnsAttemptDetailsWithoutChangingFailureStatus() {
        for (int attempts : new int[] {1, 5}) {
            var data = new PhoneVerificationFailureResponse(
                    attempts, 5 - attempts, attempts == 5 ? "FAILED" : "PENDING");
            var response = new GlobalExceptionHandler().handlePhoneVerificationFailure(
                    new PhoneVerificationFailedException("Phone verification code incorrect", data));
            assertEquals(400, response.getStatusCode().value());
            var body = response.getBody();
            assertNotNull(body);
            assertFalse(body.isSuccess());
            assertEquals("PHONE_VERIFICATION_FAILED", body.getErrorCode());
            assertEquals(data, body.getData());
        }
    }

    @Test
    void existingFailureFactoryStillHasNoData() {
        assertNull(ApiResponse.fail("BUSINESS_ERROR", "Invalid request").getData());
    }
}
