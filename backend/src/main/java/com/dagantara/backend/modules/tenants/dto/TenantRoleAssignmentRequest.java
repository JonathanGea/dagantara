package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TenantRoleAssignmentRequest(
        @NotNull UUID roleId,
        @Size(max = 32) String phoneNumber
) {
    @Override
    public String toString() {
        return "TenantRoleAssignmentRequest[roleId=" + roleId + "]";
    }
}
