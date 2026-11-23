package com.dagantara.backend.modules.users.dto;

import com.dagantara.backend.modules.users.entity.UserAccount;
import java.util.UUID;

public record UserDetailsResponse(UUID id, UUID tenantId, UUID roleId, String username,
                                  String email, String fullName, String status) {
    public static UserDetailsResponse from(UserAccount user) {
        return new UserDetailsResponse(user.getId(), user.getTenantId(), user.getRoleId(),
                user.getUsername(), user.getEmail(), user.getFullName(), user.getStatus());
    }
}
