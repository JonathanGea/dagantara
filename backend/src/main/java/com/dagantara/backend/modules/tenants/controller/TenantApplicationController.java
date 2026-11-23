package com.dagantara.backend.modules.tenants.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.tenants.dto.CompleteTenantApplicationRequest;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantResponse;
import com.dagantara.backend.modules.tenants.dto.SubmitTenantApplicationRequest;
import com.dagantara.backend.modules.tenants.dto.TenantApplicationResponse;
import com.dagantara.backend.modules.tenants.dto.TenantApplicationResponse.Status;
import com.dagantara.backend.modules.tenants.dto.UpdateTenantProposalRequest;
import com.dagantara.backend.modules.tenants.service.TenantApplicationService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping(ApiRoutes.V1 + "/tenant-applications")
public class TenantApplicationController {
    private final TenantApplicationService applications;

    public TenantApplicationController(TenantApplicationService applications) {
        this.applications = applications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Submission> submit(@Valid @RequestBody SubmitTenantApplicationRequest request,
                                          HttpServletRequest http) {
        UUID id = applications.submit(request, http.getServerName());
        return ApiResponse.success("Tenant application submitted", new Submission(id, Status.IN_REVIEW));
    }

    @GetMapping
    public ApiResponse<List<TenantApplicationResponse>> list(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Tenant applications", applications.list(actor, page));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantApplicationResponse> detail(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant application", applications.detail(actor, id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TenantApplicationResponse> update(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantProposalRequest request) {
        return ApiResponse.success("Proposal updated", applications.update(actor, id, request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<TenantApplicationResponse> close(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id) {
        return ApiResponse.success("Tenant application closed", applications.close(actor, id));
    }

    @PostMapping("/{id}/provision")
    public ResponseEntity<ApiResponse<ProvisionTenantResponse>> complete(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID id,
            @Valid @RequestBody CompleteTenantApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant provisioned", applications.complete(actor, id, request)));
    }

    public record Submission(UUID id, Status status) {
    }
}
