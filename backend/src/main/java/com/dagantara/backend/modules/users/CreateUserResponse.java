package com.dagantara.backend.modules.users;

import java.util.UUID;

public record CreateUserResponse(UUID id, UUID tenantId, UUID roleId, String username) {
    static CreateUserResponse from(UserAccount user) {
        return new CreateUserResponse(user.getId(), user.getTenantId(),
                user.getRoleId(), user.getUsername());
    }
}
