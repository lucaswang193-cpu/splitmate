package com.splitmate.exception;

public class LoginRateLimitedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LoginRateLimitedException() {
        super("Too many failed login attempts. Please try again later");
    }
}
