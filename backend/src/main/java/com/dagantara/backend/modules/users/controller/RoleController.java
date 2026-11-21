package com.dagantara.backend.modules.users.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.users.dto.CreateRoleRequest;
import com.dagantara.backend.modules.users.dto.PermissionsRequest;
import com.dagantara.backend.modules.users.dto.PermissionsResponse;
import com.dagantara.backend.modules.users.dto.RoleResponse;
import com.dagantara.backend.modules.users.service.RoleService;
import jakarta.validation.Valid;
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
        return ApiResponse.success("Role created", roles.create(jwt, request.code(), request.name()));
    }

    @PutMapping(path = "/{roleId}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PermissionsResponse> replacePermissions(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable UUID roleId,
                                                               @Valid @RequestBody PermissionsRequest request) {
        return ApiResponse.success("Permissions updated",
                new PermissionsResponse(roleId, roles.replacePermissions(jwt, roleId, request.permissionCodes())));
    }
}
