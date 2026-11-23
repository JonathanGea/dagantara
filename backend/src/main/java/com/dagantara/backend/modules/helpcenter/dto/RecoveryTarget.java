package com.dagantara.backend.modules.helpcenter.dto;

import java.util.UUID;

public record RecoveryTarget(UUID tenantId, UUID accountId, String phoneNumber) {
    @Override
    public String toString() {
        return "RecoveryTarget[tenantId=" + tenantId + ", accountId=" + accountId + "]";
    }
}
