package com.dagantara.backend.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PermissionsRequest(
        @NotNull @Size(max = 100) Set<@NotBlank @Size(max = 128) String> permissionCodes
) {
}
