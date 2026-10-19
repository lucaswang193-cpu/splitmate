package com.splitmate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlinkGoogleRequest(
        @NotBlank @Size(max = 255) String currentPassword) {

}
