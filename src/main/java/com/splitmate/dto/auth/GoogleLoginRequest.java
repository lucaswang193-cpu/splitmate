package com.splitmate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
    @NotBlank
        @Size(max = 16384)
        String credential
) {

}
