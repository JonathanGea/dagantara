package com.dagantara.backend.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class PlatformUserDtos {
    private PlatformUserDtos() {
    }

    public enum Status { ACTIVE, INACTIVE, ARCHIVED }

    public record CreateRequest(@NotBlank @Size(max = 128) String username, @NotNull UUID roleId) {
    }

    public record AssignRoleRequest(@NotNull UUID roleId) {
    }

    public record User(UUID id, String username, UUID roleId, String roleCode, Status status,
                       boolean initialPlatformAdmin) {
    }

    public record Credentials(UUID id, String username, UUID roleId, String temporaryPassword) {
        @Override
        public String toString() {
            return "Credentials[id=" + id + ", roleId=" + roleId + "]";
        }
    }
}
