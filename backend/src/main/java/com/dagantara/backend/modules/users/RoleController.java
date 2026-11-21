package com.dagantara.backend.modules.users;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/roles")
public class RoleController {
    private final RoleService roles;

    RoleController(RoleService roles) {
        this.roles = roles;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoleResponse> create(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateRoleRequest request) {
        UserManagementRepository.Role role = roles.create(jwt, request.code(), request.name());
        return ApiResponse.success("Role created", new RoleResponse(role.id(), role.code(), role.name()));
    }

    @PutMapping(path = "/{roleId}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PermissionsResponse> replacePermissions(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable UUID roleId,
                                                               @Valid @RequestBody PermissionsRequest request) {
        return ApiResponse.success("Permissions updated",
                new PermissionsResponse(roleId, roles.replacePermissions(jwt, roleId, request.permissionCodes())));
    }

    public record CreateRoleRequest(@NotBlank @Size(max = 128) String code,
                                    @NotBlank @Size(max = 255) String name) {
    }

    public record PermissionsRequest(@NotNull @Size(max = 100) Set<@NotBlank @Size(max = 128) String> permissionCodes) {
    }

    public record RoleResponse(UUID id, String code, String name) {
    }

    public record PermissionsResponse(UUID roleId, Set<String> permissionCodes) {
    }
}
