package com.dagantara.backend.modules.tenants.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.tenants.dto.TenantAccountRequest;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.Created;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.PasswordReset;
import com.dagantara.backend.modules.tenants.dto.TenantRoleAssignmentRequest;
import com.dagantara.backend.modules.tenants.service.TenantAccountManagementService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping({ApiRoutes.V1 + "/tenant/users", "/{shopSlug:[a-z0-9-]+}" + ApiRoutes.V1 + "/tenant/users"})
public class TenantAccountController {
    private final TenantAccountManagementService management;

    public TenantAccountController(TenantAccountManagementService management) {
        this.management = management;
    }

    @GetMapping
    public ApiResponse<List<TenantAccountResponse>> list(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Tenant accounts", management.listAccounts(actor, page));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantAccountResponse> detail(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant account", management.accountDetail(actor, id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Created>> create(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @Valid @RequestBody TenantAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant account created", management.createAccount(actor, request)));
    }

    @PutMapping("/{id}/role")
    public ApiResponse<TenantAccountResponse> assignRole(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id,
            @Valid @RequestBody TenantRoleAssignmentRequest request) {
        return ApiResponse.success("Role assigned", management.assignRole(actor, id, request));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<TenantAccountResponse> deactivate(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant account deactivated", management.deactivate(actor, id));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<TenantAccountResponse> activate(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant account activated", management.activate(actor, id));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<PasswordReset>> resetPassword(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant password reset", management.resetPassword(actor, id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<TenantAccountResponse> archive(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant account archived", management.archive(actor, id));
    }
}
