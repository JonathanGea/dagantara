package com.dagantara.backend.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class PlatformRoleDtos {
    private PlatformRoleDtos() {
    }

    public record CreateRequest(@NotBlank @Size(max = 128) String code, @NotBlank @Size(max = 255) String name) {
    }

    public record PermissionsRequest(@NotNull @Size(max = 100) Set<@NotBlank @Size(max = 128) String> permissionCodes) {
    }

    public record Role(UUID id, String code, String name, Set<String> permissionCodes) {
    }
}
