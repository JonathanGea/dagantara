package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TenantRolePermissionsRequest(
        @NotNull Set<@NotBlank @Size(max = 128) String> permissions
) {
}
