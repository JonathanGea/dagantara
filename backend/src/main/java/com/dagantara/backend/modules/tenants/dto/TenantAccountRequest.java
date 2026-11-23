package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TenantAccountRequest(
        @NotBlank @Size(max = 128) String username,
        @NotNull UUID roleId,
        @Size(max = 32) String phoneNumber
) {
    @Override
    public String toString() {
        return "TenantAccountRequest[roleId=" + roleId + "]";
    }
}
