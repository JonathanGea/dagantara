package com.dagantara.backend.modules.auth.dto;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                            boolean passwordChangeRequired) {
    @Override
    public String toString() {
        return "LoginResponse[expiresAt=" + expiresAt + ", passwordChangeRequired=" + passwordChangeRequired + "]";
    }
}
