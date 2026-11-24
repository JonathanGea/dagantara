package com.dagantara.backend.modules.tenants.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantRequest;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantResponse;
import com.dagantara.backend.modules.tenants.dto.TenantResponse;
import com.dagantara.backend.modules.tenants.service.TenantManagementService;
import com.dagantara.backend.modules.tenants.service.TenantProvisioningService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping(ApiRoutes.V1 + "/tenants")
public class TenantController {
    private final TenantProvisioningService provisioning;
    private final TenantManagementService management;

    public TenantController(TenantProvisioningService provisioning, TenantManagementService management) {
        this.provisioning = provisioning;
        this.management = management;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProvisionTenantResponse>> provision(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @Valid @RequestBody ProvisionTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant provisioned", provisioning.provision(actor, request)));
    }

    @GetMapping
    public ApiResponse<List<TenantResponse>> list(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                                   Pageable page) {
        return ApiResponse.paged("Tenants", management.list(actor, page));
    }

    @PostMapping("/{tenantId}/deactivate")
    public ApiResponse<TenantResponse> deactivate(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                                   @PathVariable UUID tenantId) {
        return ApiResponse.success("Tenant deactivated", management.deactivate(actor, tenantId));
    }

    @PostMapping("/{tenantId}/reactivate")
    public ApiResponse<TenantResponse> reactivate(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                                   @PathVariable UUID tenantId) {
        return ApiResponse.success("Tenant reactivated", management.reactivate(actor, tenantId));
    }
}
