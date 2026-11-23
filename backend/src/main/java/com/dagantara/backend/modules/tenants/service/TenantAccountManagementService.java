package com.dagantara.backend.modules.tenants.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.tenants.TenantPermission;
import com.dagantara.backend.modules.tenants.dto.TenantAccountRequest;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.Created;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.PasswordReset;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.Status;
import com.dagantara.backend.modules.tenants.dto.TenantRoleAssignmentRequest;
import com.dagantara.backend.modules.tenants.dto.TenantRolePermissionsRequest;
import com.dagantara.backend.modules.tenants.dto.TenantRoleRequest;
import com.dagantara.backend.modules.tenants.dto.TenantRoleResponse;
import com.dagantara.backend.modules.tenants.repository.TenantAccountRepository;
import com.dagantara.backend.modules.tenants.repository.TenantAccountRepository.Manager;
import com.dagantara.backend.modules.tenants.repository.TenantRoleRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.dagantara.backend.modules.tenants.TenantPermission.ACTIVATE_USERS;
import static com.dagantara.backend.modules.tenants.TenantPermission.ARCHIVE_USERS;
import static com.dagantara.backend.modules.tenants.TenantPermission.ASSIGN_ROLES;
import static com.dagantara.backend.modules.tenants.TenantPermission.CREATE_ROLES;
import static com.dagantara.backend.modules.tenants.TenantPermission.CREATE_USERS;
import static com.dagantara.backend.modules.tenants.TenantPermission.DEACTIVATE_USERS;
import static com.dagantara.backend.modules.tenants.TenantPermission.READ_USERS;
import static com.dagantara.backend.modules.tenants.TenantPermission.RESET_PASSWORDS;
import static com.dagantara.backend.modules.tenants.TenantPermission.UPDATE_ROLE_PERMISSIONS;

@Service
@Transactional
public class TenantAccountManagementService {
    private static final String ADMIN_ROLE = "TENANT_ADMIN";

    private final SecureRandom random = new SecureRandom();
    private final TenantAccountRepository accounts;
    private final TenantRoleRepository roles;
    private final PasswordEncoder encoder;

    public TenantAccountManagementService(TenantAccountRepository accounts, TenantRoleRepository roles,
                                          PasswordEncoder encoder) {
        this.accounts = accounts;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TenantAccountResponse> listAccounts(AccountSession session, Pageable page) {
        require(manager(session), READ_USERS);
        return accounts.list(session.tenantId(), PageableRules.enforce(page, Set.of()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TenantAccountResponse accountDetail(AccountSession session, UUID id) {
        require(manager(session), READ_USERS);
        return account(session.tenantId(), id);
    }

    public Created createAccount(AccountSession session, TenantAccountRequest request) {
        Manager manager = lockManager(session);
        require(manager, CREATE_USERS);
        require(manager, ASSIGN_ROLES);
        UUID tenantId = session.tenantId();
        TenantRoleResponse role = role(tenantId, request.roleId());
        requireOwnerForAdmin(manager, role.code());
        requireDelegable(manager, role.permissions());
        String phone = appointmentPhone(role.code(), request.phoneNumber());
        String password = temporaryPassword();
        UUID id = accounts.insert(tenantId, role.id(), request.username(), phone, encoder.encode(password));
        return new Created(account(tenantId, id), password);
    }

    public TenantAccountResponse assignRole(AccountSession session, UUID id, TenantRoleAssignmentRequest request) {
        Manager manager = lockManager(session);
        require(manager, ASSIGN_ROLES);
        UUID tenantId = session.tenantId();
        TenantAccountResponse account = account(tenantId, id);
        TenantRoleResponse replacement = role(tenantId, request.roleId());
        if (account.owner()) {
            throw protectedAccount();
        }
        requireOwnerForAdmin(manager, account.roleCode());
        requireOwnerForAdmin(manager, replacement.code());
        requireNotArchived(account);
        requireDelegable(manager, role(tenantId, account.roleId()).permissions());
        requireDelegable(manager, replacement.permissions());
        if (replacement.id().equals(account.roleId())) {
            return account;
        }
        String phone = appointmentPhone(replacement.code(), request.phoneNumber());
        accounts.assignRole(tenantId, id, replacement.id(), phone);
        return account(tenantId, id);
    }

    public TenantAccountResponse deactivate(AccountSession session, UUID id) {
        return changeStatus(session, id, DEACTIVATE_USERS, Status.INACTIVE);
    }

    public TenantAccountResponse activate(AccountSession session, UUID id) {
        return changeStatus(session, id, ACTIVATE_USERS, Status.ACTIVE);
    }

    public TenantAccountResponse archive(AccountSession session, UUID id) {
        return changeStatus(session, id, ARCHIVE_USERS, Status.ARCHIVED);
    }

    public PasswordReset resetPassword(AccountSession session, UUID id) {
        Manager manager = lockManager(session);
        require(manager, RESET_PASSWORDS);
        UUID tenantId = session.tenantId();
        TenantAccountResponse target = account(tenantId, id);
        requireStaff(target);
        requireNotArchived(target);
        requireDelegable(manager, role(tenantId, target.roleId()).permissions());
        String password = temporaryPassword();
        accounts.resetPassword(tenantId, id, encoder.encode(password));
        return new PasswordReset(password);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TenantRoleResponse> listRoles(AccountSession session, Pageable page) {
        requireRoleRead(manager(session));
        return roles.list(session.tenantId(), PageableRules.enforce(page, Set.of()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TenantRoleResponse roleDetail(AccountSession session, UUID id) {
        requireRoleRead(manager(session));
        return role(session.tenantId(), id);
    }

    public TenantRoleResponse createRole(AccountSession session, TenantRoleRequest request) {
        Manager manager = lockManager(session);
        require(manager, CREATE_ROLES);
        if (ADMIN_ROLE.equals(request.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "RESERVED_ROLE", "Admin Tenant is a built-in role");
        }
        UUID tenantId = session.tenantId();
        return role(tenantId, roles.insert(tenantId, request.code(), request.name()));
    }

    public TenantRoleResponse replacePermissions(AccountSession session, UUID id,
                                                TenantRolePermissionsRequest request) {
        Manager manager = lockManager(session);
        require(manager, UPDATE_ROLE_PERMISSIONS);
        UUID tenantId = session.tenantId();
        TenantRoleResponse role = role(tenantId, id);
        if (ADMIN_ROLE.equals(role.code())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PROTECTED_ROLE",
                    "Permissions for the built-in Admin Tenant role cannot be changed");
        }
        if (!TenantPermission.CODES.containsAll(request.permissions())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERMISSIONS",
                    "Requested permissions must come from the tenant permission catalogue");
        }
        requireDelegable(manager, role.permissions());
        requireDelegable(manager, request.permissions());
        roles.replacePermissions(tenantId, id, request.permissions());
        return role(tenantId, id);
    }

    private TenantAccountResponse changeStatus(AccountSession session, UUID id, TenantPermission permission,
                                               Status status) {
        Manager manager = lockManager(session);
        require(manager, permission);
        UUID tenantId = session.tenantId();
        TenantAccountResponse target = account(tenantId, id);
        requireStaff(target);
        if (target.status() == status) {
            return target;
        }
        requireNotArchived(target);
        accounts.updateStatus(tenantId, id, status);
        return account(tenantId, id);
    }

    private Manager lockManager(AccountSession session) {
        requireTenant(session);
        // Serialize tenant management writes so grants and account status stay current throughout authorization.
        accounts.lockManagement(session.tenantId());
        return manager(session);
    }

    private Manager manager(AccountSession session) {
        requireTenant(session);
        return accounts.findManager(session).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Session is no longer valid"));
    }

    private static void requireTenant(AccountSession session) {
        if (session.tenantId() == null) {
            throw forbidden();
        }
    }

    private static void require(Manager manager, TenantPermission permission) {
        if (!manager.permissions().contains(permission.code())) {
            throw forbidden();
        }
    }

    private static void requireRoleRead(Manager manager) {
        if (!manager.permissions().contains(ASSIGN_ROLES.code())
                && !manager.permissions().contains(CREATE_ROLES.code())
                && !manager.permissions().contains(UPDATE_ROLE_PERMISSIONS.code())) {
            throw forbidden();
        }
    }

    private static void requireDelegable(Manager manager, Collection<String> permissions) {
        if (!manager.permissions().containsAll(permissions)) {
            throw forbidden();
        }
    }

    private static void requireOwnerForAdmin(Manager manager, String roleCode) {
        if (ADMIN_ROLE.equals(roleCode) && !manager.owner()) {
            throw protectedAccount();
        }
    }

    private static void requireStaff(TenantAccountResponse account) {
        if (account.owner() || ADMIN_ROLE.equals(account.roleCode())) {
            throw protectedAccount();
        }
    }

    private static void requireNotArchived(TenantAccountResponse account) {
        if (account.status() == Status.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ARCHIVED", "Archived account cannot be changed");
        }
    }

    private static String appointmentPhone(String roleCode, String phoneNumber) {
        if (!ADMIN_ROLE.equals(roleCode)) {
            return null;
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_NUMBER_REQUIRED",
                    "A recovery phone number is required to appoint an Admin Tenant");
        }
        return phoneNumber.strip();
    }

    private TenantAccountResponse account(UUID tenantId, UUID id) {
        return accounts.find(tenantId, id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tenant account not found"));
    }

    private TenantRoleResponse role(UUID tenantId, UUID id) {
        return roles.find(tenantId, id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tenant role not found"));
    }

    private String temporaryPassword() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient tenant permissions");
    }

    private static ApiException protectedAccount() {
        return new ApiException(HttpStatus.FORBIDDEN, "PROTECTED_ACCOUNT", "Tenant administrator account is protected");
    }
}
