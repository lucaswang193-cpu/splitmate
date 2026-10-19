package com.splitmate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkGoogleRequest(
        @NotBlank @Size(max = 16384) String credential,
        @NotBlank @Size(max = 255) String currentPassword) {

}
