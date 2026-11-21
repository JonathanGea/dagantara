package com.dagantara.backend.modules.users;

import com.dagantara.backend.common.audit.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount extends AuditableEntity {
    public static final String ACTIVE = "ACTIVE";

    private UUID tenantId;
    private UUID roleId;
    private String username;
    private String email;
    private String fullName;
    private String passwordHash;
    private String accountType;
    private String status;
    private UUID createdBy;

    protected UserAccount() {
    }

    UserAccount(UserAccount creator, CreateUserRequest request, String passwordHash) {
        tenantId = creator.tenantId;
        roleId = request.roleId();
        username = request.username();
        email = request.email();
        fullName = request.fullName();
        this.passwordHash = passwordHash;
        accountType = creator.accountType;
        status = ACTIVE;
        createdBy = creator.getId();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    String getAccountType() {
        return accountType;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
