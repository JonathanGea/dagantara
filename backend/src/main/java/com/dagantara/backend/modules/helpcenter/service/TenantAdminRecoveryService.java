package com.dagantara.backend.modules.helpcenter.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.helpcenter.dto.RecoveryLookupRequest;
import com.dagantara.backend.modules.helpcenter.dto.RecoveryTarget;
import com.dagantara.backend.modules.helpcenter.dto.VerifiedResetRequest;
import com.dagantara.backend.modules.helpcenter.repository.RecoveryRepository;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.PasswordReset;
import com.dagantara.backend.modules.tenants.repository.TenantAccountRepository;
import com.dagantara.backend.modules.tenants.service.TenantLoginAddress;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAdminRecoveryService {
    private static final String RECOVERY_PERMISSION = "tenants.admins.password.reset";

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accounts;
    private final RolePermissionRepository permissions;
    private final TenantAccountRepository tenantAccounts;
    private final TenantLoginAddress addresses;
    private final RecoveryRepository recovery;
    private final PasswordEncoder encoder;

    public TenantAdminRecoveryService(AccountRepository accounts, RolePermissionRepository permissions,
                                      TenantAccountRepository tenantAccounts, TenantLoginAddress addresses,
                                      RecoveryRepository recovery, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.permissions = permissions;
        this.tenantAccounts = tenantAccounts;
        this.addresses = addresses;
        this.recovery = recovery;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RecoveryTarget lookup(AccountSession actor, RecoveryLookupRequest request) {
        requireRecoveryPermission(actor);
        Address address = addresses.provisioningAddress(request.loginUrl()).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_URL", "Use a valid tenant login URL"));
        return recovery.findTarget(address, request.username()).orElseThrow(TenantAdminRecoveryService::notFound);
    }

    @Transactional
    public PasswordReset reset(AccountSession actor, UUID tenantId, UUID accountId, VerifiedResetRequest request) {
        requirePlatformAccount(actor);
        // Serialize permission changes and tenant role changes with the recovery decision.
        accounts.lockPlatformAdminRole();
        requireRecoveryPermission(actor);
        tenantAccounts.lockManagement(tenantId);
        if (!recovery.isRecoverableAdmin(tenantId, accountId)) {
            throw notFound();
        }

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tenantAccounts.resetPassword(tenantId, accountId, encoder.encode(password));
        recovery.recordReset(actor.id(), tenantId, accountId, request.verificationMethod());
        return new PasswordReset(password);
    }

    private void requireRecoveryPermission(AccountSession actor) {
        requirePlatformAccount(actor);
        accounts.findSession(actor.id()).filter(actor::equals).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Session is no longer valid"));
        if (actor.passwordChangeRequired() || !permissions.hasPlatformPermission(actor.id(), RECOVERY_PERMISSION)) {
            throw forbidden();
        }
    }

    private static void requirePlatformAccount(AccountSession actor) {
        if (actor.tenantId() != null) {
            throw forbidden();
        }
    }

    private static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Tenant administrator recovery permission is required");
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recoverable tenant administrator not found");
    }
}
