package com.dagantara.backend.modules.tenants.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantRequest;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantResponse;
import com.dagantara.backend.modules.tenants.service.TenantProvisioningService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public TenantController(TenantProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProvisionTenantResponse>> provision(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @Valid @RequestBody ProvisionTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant provisioned", provisioning.provision(actor, request)));
    }
}
