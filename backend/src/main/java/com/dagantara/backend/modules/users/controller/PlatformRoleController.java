package com.dagantara.backend.modules.users.controller;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.dto.PlatformRoleDtos.CreateRequest;
import com.dagantara.backend.modules.users.dto.PlatformRoleDtos.PermissionsRequest;
import com.dagantara.backend.modules.users.dto.PlatformRoleDtos.Role;
import com.dagantara.backend.modules.users.service.PlatformOperatorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/roles")
public class PlatformRoleController {
    private final PlatformOperatorService operators;

    public PlatformRoleController(PlatformOperatorService operators) {
        this.operators = operators;
    }

    @GetMapping
    public ApiResponse<List<Role>> list(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor) {
        return ApiResponse.success("Platform roles found", operators.listRoles(actor));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<String>> permissions(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor) {
        return ApiResponse.success("Delegable permissions found", operators.listAssignablePermissions(actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Role> create(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                    @Valid @RequestBody CreateRequest request) {
        return ApiResponse.success("Platform role created", operators.createRole(actor, request.code(), request.name()));
    }

    @PutMapping("/{roleId}/permissions")
    public ApiResponse<Role> replacePermissions(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID roleId,
            @Valid @RequestBody PermissionsRequest request) {
        return ApiResponse.success("Role permissions updated",
                operators.replacePermissions(actor, roleId, request.permissionCodes()));
    }
}
