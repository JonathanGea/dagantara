package com.dagantara.backend.modules.users.dto;

import java.util.UUID;

public record RoleAssignmentResponse(UUID userId, UUID roleId) {
}
