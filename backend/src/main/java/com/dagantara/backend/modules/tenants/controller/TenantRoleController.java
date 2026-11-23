package com.dagantara.backend.modules.tenants.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.tenants.dto.TenantRolePermissionsRequest;
import com.dagantara.backend.modules.tenants.dto.TenantRoleRequest;
import com.dagantara.backend.modules.tenants.dto.TenantRoleResponse;
import com.dagantara.backend.modules.tenants.service.TenantAccountManagementService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping({ApiRoutes.V1 + "/tenant/roles", "/{shopSlug:[a-z0-9-]+}" + ApiRoutes.V1 + "/tenant/roles"})
public class TenantRoleController {
    private final TenantAccountManagementService management;

    public TenantRoleController(TenantAccountManagementService management) {
        this.management = management;
    }

    @GetMapping
    public ApiResponse<List<TenantRoleResponse>> list(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Tenant roles", management.listRoles(actor, page));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantRoleResponse> detail(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant role", management.roleDetail(actor, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantRoleResponse> create(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @Valid @RequestBody TenantRoleRequest request) {
        return ApiResponse.success("Tenant role created", management.createRole(actor, request));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<TenantRoleResponse> replacePermissions(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id,
            @Valid @RequestBody TenantRolePermissionsRequest request) {
        return ApiResponse.success("Tenant role permissions replaced", management.replacePermissions(actor, id, request));
    }
}
