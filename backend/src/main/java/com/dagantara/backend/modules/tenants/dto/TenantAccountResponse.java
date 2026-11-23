package com.dagantara.backend.modules.tenants.dto;

import java.util.UUID;

public record TenantAccountResponse(UUID id, UUID tenantId, String username, UUID roleId,
                                     String roleCode, Status status, boolean owner) {
    public enum Status {
        ACTIVE, INACTIVE, ARCHIVED
    }

    public record Created(TenantAccountResponse account, String temporaryPassword) {
        @Override
        public String toString() {
            return "Created[accountId=" + account.id() + "]";
        }
    }

    public record PasswordReset(String temporaryPassword) {
        @Override
        public String toString() {
            return "PasswordReset";
        }
    }
}
