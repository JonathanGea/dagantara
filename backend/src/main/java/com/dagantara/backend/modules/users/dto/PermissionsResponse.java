package com.dagantara.backend.modules.users.dto;

import java.util.Set;
import java.util.UUID;

public record PermissionsResponse(UUID roleId, Set<String> permissionCodes) {
}
