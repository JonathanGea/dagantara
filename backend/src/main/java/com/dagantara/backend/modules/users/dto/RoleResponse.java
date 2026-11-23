package com.dagantara.backend.modules.users.dto;

import java.util.UUID;

public record RoleResponse(UUID id, String code, String name) {
}
