package com.splitmate.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name must be less than 100 characters")
    String name,

    String description,

    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Base currency must be 3 characters")
    String baseCurrency
) {

}
