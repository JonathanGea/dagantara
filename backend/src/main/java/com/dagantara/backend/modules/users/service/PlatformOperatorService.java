package com.dagantara.backend.modules.users.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.dto.PlatformRoleDtos.Role;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.Credentials;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.Status;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.User;
import com.dagantara.backend.modules.users.repository.AccountRepository;
import com.dagantara.backend.modules.users.repository.PlatformOperatorRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformOperatorService {
    private static final String ADMIN_ROLE = "SUPERADMIN";
    private static final String USER_CREATE = "users.create";
    private static final String USER_READ = "users.read";
    private static final String ROLE_ASSIGN = "users.role.assign";
    private static final String ROLE_CREATE = "roles.create";
    private static final String ROLE_PERMISSIONS_UPDATE = "roles.permissions.update";

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accounts;
    private final PlatformOperatorRepository operators;
    private final PasswordEncoder passwordEncoder;

    public PlatformOperatorService(AccountRepository accounts, PlatformOperatorRepository operators,
                                   PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.operators = operators;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Credentials createUser(AccountSession session, String username, UUID roleId) {
        Actor actor = authorizeWrite(session, USER_CREATE);
        actor.requireAny(ROLE_ASSIGN);
        Role role = assignableRole(roleId);
        requireAdminRoleAuthority(actor, role.code());
        actor.requireDelegation(role.permissionCodes());
        String password = temporaryPassword();
        UUID id = operators.createUser(username, roleId, passwordEncoder.encode(password));
        return new Credentials(id, username, roleId, password);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<User> listUsers(AccountSession session, Pageable page) {
        actor(session).requireAny(USER_READ);
        return operators.listUsers(PageableRules.enforce(page, Set.of()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public User getUser(AccountSession session, UUID id) {
        actor(session).requireAny(USER_READ);
        return user(id);
    }

    @Transactional
    public User assignRole(AccountSession session, UUID userId, UUID roleId) {
        Actor actor = authorizeWrite(session, ROLE_ASSIGN);
        User user = manageableUser(actor, userId);
        requireAdminRoleAuthority(actor, user.roleCode());
        Role role = assignableRole(roleId);
        requireAdminRoleAuthority(actor, role.code());
        actor.requireDelegation(role.permissionCodes());
        operators.assignRole(userId, roleId);
        return user(userId);
    }

    @Transactional
    public Credentials resetPassword(AccountSession session, UUID userId) {
        Actor actor = authorizeWrite(session, "users.password.reset");
        User user = manageableUser(actor, userId);
        String password = temporaryPassword();
        operators.resetPassword(userId, passwordEncoder.encode(password));
        return new Credentials(userId, user.username(), user.roleId(), password);
    }

    @Transactional
    public User changeStatus(AccountSession session, UUID userId, Status next) {
        String permission = switch (next) {
            case ACTIVE -> "users.activate";
            case INACTIVE -> "users.deactivate";
            case ARCHIVED -> "users.archive";
        };
        Actor actor = authorizeWrite(session, permission);
        User user = manageableUser(actor, userId);
        if (user.status() == next) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_ACCOUNT_TRANSITION",
                    "The account already has the requested status");
        }
        operators.changeStatus(userId, next);
        return user(userId);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Role> listRoles(AccountSession session) {
        actor(session).requireAny(USER_CREATE, ROLE_ASSIGN, ROLE_CREATE, ROLE_PERMISSIONS_UPDATE);
        return operators.listRoles();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<String> listAssignablePermissions(AccountSession session) {
        Actor actor = actor(session);
        actor.requireAny(ROLE_PERMISSIONS_UPDATE);
        return actor.permissions().stream().sorted().toList();
    }

    @Transactional
    public Role createRole(AccountSession session, String code, String name) {
        authorizeWrite(session, ROLE_CREATE);
        if (ADMIN_ROLE.equalsIgnoreCase(code) || "TENANT_ADMIN".equalsIgnoreCase(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESERVED_ROLE_CODE", "The role code is reserved");
        }
        return operators.createRole(code, name);
    }

    @Transactional
    public Role replacePermissions(AccountSession session, UUID roleId, Set<String> permissions) {
        Actor actor = authorizeWrite(session, ROLE_PERMISSIONS_UPDATE);
        Role role = operators.findRole(roleId).orElseThrow(PlatformOperatorService::notFound);
        if (ADMIN_ROLE.equals(role.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "SYSTEM_ROLE_PROTECTED", "System role permissions are fixed");
        }
        actor.requireDelegation(role.permissionCodes());
        if (!operators.permissionCatalog().containsAll(permissions)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERMISSION", "Select permissions from the catalog");
        }
        actor.requireDelegation(permissions);
        operators.replacePermissions(roleId, permissions);
        return new Role(role.id(), role.code(), role.name(), permissions);
    }

    private Actor authorizeWrite(AccountSession session, String permission) {
        requirePlatformScope(session);
        // All platform management writes share this lock, so delegation and mutation see the same authority.
        accounts.lockPlatformAdminRole();
        Actor actor = actor(session);
        actor.requireAny(permission);
        return actor;
    }

    private Actor actor(AccountSession session) {
        requirePlatformScope(session);
        User user = operators.findActor(session).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid session"));
        Role role = operators.findRole(user.roleId()).orElseThrow(PlatformOperatorService::notFound);
        return new Actor(user.initialPlatformAdmin(), role.permissionCodes());
    }

    private User manageableUser(Actor actor, UUID id) {
        User user = user(id);
        if (user.initialPlatformAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INITIAL_ADMIN_PROTECTED",
                    "The initial platform administrator is protected");
        }
        if (user.status() == Status.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ARCHIVED", "The account is archived");
        }
        actor.requireDelegation(assignableRole(user.roleId()).permissionCodes());
        return user;
    }

    private User user(UUID id) {
        return operators.findUser(id).orElseThrow(PlatformOperatorService::notFound);
    }

    private Role assignableRole(UUID id) {
        return operators.findRole(id).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Select a role in the platform scope"));
    }

    private static void requirePlatformScope(AccountSession session) {
        if (session.tenantId() != null) {
            throw forbidden("A platform account is required");
        }
    }

    private static void requireAdminRoleAuthority(Actor actor, String roleCode) {
        if (ADMIN_ROLE.equals(roleCode) && !actor.initial()) {
            throw forbidden("Only the initial platform administrator may assign or remove the platform administrator role");
        }
    }

    private String temporaryPassword() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    private static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private record Actor(boolean initial, Set<String> permissions) {
        private void requireAny(String... required) {
            for (String permission : required) {
                if (permissions.contains(permission)) {
                    return;
                }
            }
            throw forbidden("The operation requires a platform permission");
        }

        private void requireDelegation(Set<String> managed) {
            if (!permissions.containsAll(managed)) {
                throw forbidden("The role contains permissions outside your authority");
            }
        }
    }
}
