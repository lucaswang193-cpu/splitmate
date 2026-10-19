package com.splitmate.exception;

import com.splitmate.dto.auth.PhoneVerificationFailureResponse;

public class PhoneVerificationFailedException extends BusinessException{

    private final PhoneVerificationFailureResponse data;

    public PhoneVerificationFailedException(String message) {
        this(message, null);
    }

    public PhoneVerificationFailedException(String message, PhoneVerificationFailureResponse data) {
        super(message);
        this.data = data;
    }

    public PhoneVerificationFailureResponse getData() {
        return data;
    }
}
