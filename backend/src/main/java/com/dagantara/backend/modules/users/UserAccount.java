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

    UserAccount(UUID creatorId, UUID tenantId, UUID roleId, String username,
                String email, String fullName, String passwordHash) {
        this.tenantId = tenantId;
        this.roleId = roleId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        accountType = tenantId == null ? "PLATFORM" : "TENANT";
        status = ACTIVE;
        createdBy = creatorId;
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

    String getEmail() {
        return email;
    }

    String getFullName() {
        return fullName;
    }

    void assignRole(UUID roleId) {
        this.roleId = roleId;
    }
}
