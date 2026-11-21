package com.dagantara.backend.modules.users.dto;

import java.util.UUID;

public record TenantResponse(UUID id, String code, CreateUserResponse admin) {
}
