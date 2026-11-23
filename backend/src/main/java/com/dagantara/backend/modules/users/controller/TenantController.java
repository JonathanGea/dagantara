package com.dagantara.backend.modules.users.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.users.dto.CreateTenantRequest;
import com.dagantara.backend.modules.users.dto.TenantResponse;
import com.dagantara.backend.modules.users.service.TenantProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/tenants")
public class TenantController {
    private final TenantProvisioningService tenants;

    TenantController(TenantProvisioningService tenants) {
        this.tenants = tenants;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantResponse> provision(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.success("Tenant provisioned", tenants.provision(jwt, request));
    }
}
