package com.dagantara.backend.modules.tenants.dto;

import java.util.List;
import java.util.UUID;

public record TenantRoleResponse(UUID id, UUID tenantId, String code, String name, List<String> permissions) {
}
