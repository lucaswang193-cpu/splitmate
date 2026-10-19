package com.splitmate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
    @NotBlank @Size(max = 16384)
        String credential,

        @NotBlank @Size(min = 8, max = 72)
        String newPassword
) {

}
