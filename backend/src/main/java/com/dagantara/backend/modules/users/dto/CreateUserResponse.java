package com.dagantara.backend.modules.users.dto;

import com.dagantara.backend.modules.users.entity.UserAccount;
import java.util.UUID;

public record CreateUserResponse(UUID id, UUID tenantId, UUID roleId, String username) {
    public static CreateUserResponse from(UserAccount user) {
        return new CreateUserResponse(user.getId(), user.getTenantId(),
                user.getRoleId(), user.getUsername());
    }
}
