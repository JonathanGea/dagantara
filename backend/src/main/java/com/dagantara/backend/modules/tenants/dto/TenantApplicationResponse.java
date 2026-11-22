package com.dagantara.backend.modules.tenants.dto;

import java.util.UUID;

public record TenantApplicationResponse(UUID id, String storeName, String phoneNumber,
                                        String proposedDomain, Status status, UUID tenantId) {
    public enum Status {
        IN_REVIEW, CLOSED, PROVISIONED
    }
}
