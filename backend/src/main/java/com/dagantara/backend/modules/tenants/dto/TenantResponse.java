package com.dagantara.backend.modules.tenants.dto;

import java.util.UUID;

public record TenantResponse(UUID id, String name, String storeAddress, Status status) {
    public enum Status {
        ACTIVE, INACTIVE
    }
}
