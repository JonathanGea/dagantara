package com.dagantara.backend.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(max = 255) String name
) {
}
