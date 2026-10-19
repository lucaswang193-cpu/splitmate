package com.splitmate.security;

import java.util.UUID;

import com.splitmate.entity.User;

public record AuthenticatedUser(
    UUID id,
    String name,
    String email
) {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
