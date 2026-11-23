package com.dagantara.backend.common.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class UserIdentity {
    public static final String CLAIM = "identity_type";
    public static final String USER = "user";

    private UserIdentity() {
    }

    public static Optional<UUID> userId(Jwt jwt) {
        if (jwt == null || !USER.equals(jwt.getClaims().get(CLAIM)) || jwt.getSubject() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(jwt.getSubject()));
        } catch (IllegalArgumentException invalidSubject) {
            return Optional.empty();
        }
    }
}
