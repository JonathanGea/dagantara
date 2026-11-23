package com.dagantara.backend.modules.tenants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TenantPermission {
    CREATE_USERS("users.create"),
    READ_USERS("users.read"),
    ASSIGN_ROLES("users.role.assign"),
    DEACTIVATE_USERS("users.deactivate"),
    ACTIVATE_USERS("users.activate"),
    RESET_PASSWORDS("users.password.reset"),
    ARCHIVE_USERS("users.archive"),
    CREATE_ROLES("roles.create"),
    UPDATE_ROLE_PERMISSIONS("roles.permissions.update");

    public static final Set<String> CODES = Arrays.stream(values())
            .map(TenantPermission::code)
            .collect(Collectors.toUnmodifiableSet());

    private final String code;

    TenantPermission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
