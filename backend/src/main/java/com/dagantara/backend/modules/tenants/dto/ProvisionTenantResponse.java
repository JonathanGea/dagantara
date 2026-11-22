package com.dagantara.backend.modules.tenants.dto;

import java.util.UUID;

public record ProvisionTenantResponse(UUID tenantId, UUID ownerId, String ownerUsername,
                                      String loginUrl, String temporaryPassword) {
    @Override
    public String toString() {
        return "ProvisionTenantResponse[tenantId=" + tenantId + ", ownerId=" + ownerId + "]";
    }
}
