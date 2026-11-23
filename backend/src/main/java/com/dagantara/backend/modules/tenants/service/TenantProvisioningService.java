package com.dagantara.backend.modules.tenants.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantRequest;
import com.dagantara.backend.modules.tenants.dto.ProvisionTenantResponse;
import com.dagantara.backend.modules.tenants.repository.TenantRepository;
import com.dagantara.backend.modules.tenants.service.TenantLoginAddress.Address;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.repository.AccountRepository;
import com.dagantara.backend.modules.users.repository.RolePermissionRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantProvisioningService {
    private static final String PROVISION_PERMISSION = "tenants.provision";
    private final SecureRandom random = new SecureRandom();
    private final TenantRepository tenants;
    private final TenantLoginAddress addresses;
    private final RolePermissionRepository roles;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public TenantProvisioningService(TenantRepository tenants, TenantLoginAddress addresses,
                                     RolePermissionRepository roles, AccountRepository accounts,
                                     PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.addresses = addresses;
        this.roles = roles;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ProvisionTenantResponse provision(AccountSession actor, ProvisionTenantRequest request) {
        requireProvisionPermission(actor);
        return createTenant(request.storeName(), request.phoneNumber(), request.ownerUsername(), request.loginUrl());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ProvisionTenantResponse createTenant(String storeName, String phoneNumber, String username, String loginUrl) {
        Address address = addresses.provisioningAddress(loginUrl).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_URL",
                        "Use a store domain or the platform hostname with a single store slug"));

        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        tenants.insertTenant(tenantId, storeName, ownerId);
        if (!tenants.reserveAddress(tenantId, address)) {
            throw new ApiException(HttpStatus.CONFLICT, "LOGIN_URL_IN_USE", "Login URL already in use");
        }

        UUID roleId = roles.createTenantAdminRole(tenantId);
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String temporaryPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        accounts.insertTenantOwner(ownerId, tenantId, roleId, username, phoneNumber,
                passwordEncoder.encode(temporaryPassword));
        return new ProvisionTenantResponse(tenantId, ownerId, username, address.loginUrl(), temporaryPassword);
    }

    void requireProvisionPermission(AccountSession actor) {
        if (!roles.hasPlatformPermission(actor.id(), PROVISION_PERMISSION)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Tenant provisioning permission is required");
        }
    }
}
