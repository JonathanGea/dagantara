package com.dagantara.backend.modules.tenants.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.tenants.dto.CompleteTenantApplicationRequest;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantResponse;
import com.dagantara.backend.modules.tenants.dto.SubmitTenantApplicationRequest;
import com.dagantara.backend.modules.tenants.dto.TenantApplicationResponse;
import com.dagantara.backend.modules.tenants.dto.TenantApplicationResponse.Status;
import com.dagantara.backend.modules.tenants.dto.UpdateTenantProposalRequest;
import com.dagantara.backend.modules.tenants.repository.TenantApplicationRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantApplicationService {
    private final TenantApplicationRepository applications;
    private final TenantProvisioningService provisioning;
    private final TenantLoginAddress addresses;

    public TenantApplicationService(TenantApplicationRepository applications,
                                    TenantProvisioningService provisioning, TenantLoginAddress addresses) {
        this.applications = applications;
        this.provisioning = provisioning;
        this.addresses = addresses;
    }

    public UUID submit(SubmitTenantApplicationRequest request, String hostname) {
        if (!addresses.isPlatformHost(hostname)) {
            throw notFound();
        }
        return applications.insert(request.storeName(), request.phoneNumber(), domain(request.proposedDomain()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TenantApplicationResponse> list(AccountSession actor, Pageable page) {
        provisioning.requireProvisionPermission(actor);
        return applications.list(PageableRules.enforce(page, Set.of()));
    }

    public TenantApplicationResponse detail(AccountSession actor, UUID id) {
        provisioning.requireProvisionPermission(actor);
        return applications.find(id).orElseThrow(TenantApplicationService::notFound);
    }

    @Transactional
    public TenantApplicationResponse update(AccountSession actor, UUID id, UpdateTenantProposalRequest request) {
        reviewableApplication(actor, id);
        return applications.updateProposal(id, domain(request.proposedDomain()));
    }

    @Transactional
    public TenantApplicationResponse close(AccountSession actor, UUID id) {
        reviewableApplication(actor, id);
        return applications.changeStatus(id, Status.CLOSED, null);
    }

    @Transactional
    public ProvisionTenantResponse complete(AccountSession actor, UUID id, CompleteTenantApplicationRequest request) {
        TenantApplicationResponse application = reviewableApplication(actor, id);
        ProvisionTenantResponse provisioned = provisioning.createTenant(application.storeName(),
                application.phoneNumber(), request.ownerUsername(), request.loginUrl());
        applications.changeStatus(id, Status.PROVISIONED, provisioned.tenantId());
        return provisioned;
    }

    private TenantApplicationResponse reviewableApplication(AccountSession actor, UUID id) {
        provisioning.requireProvisionPermission(actor);
        TenantApplicationResponse application = applications.lock(id).orElseThrow(TenantApplicationService::notFound);
        if (application.status() != Status.IN_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_NOT_IN_REVIEW",
                    "Only an application in review can be changed or provisioned");
        }
        return application;
    }

    private String domain(String value) {
        return addresses.parseHost(value).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROPOSED_DOMAIN", "Expected a hostname"));
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tenant application not found");
    }
}
