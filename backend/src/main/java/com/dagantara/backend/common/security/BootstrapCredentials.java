package com.dagantara.backend.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * One temporary technical identity configured through environment variables.
 * Not a self-registration API and not the future domain users table.
 */
@Component
public class BootstrapCredentials {
    private final String username;
    private final String passwordHash;
    private final PasswordEncoder encoder;

    public BootstrapCredentials(
            @Value("${app.security.bootstrap.username:}") String username,
            @Value("${app.security.bootstrap.password-hash:}") String passwordHash,
            PasswordEncoder encoder
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.encoder = encoder;
    }

    public boolean matches(String attemptedUsername, String attemptedPassword) {
        if (username.isBlank() || passwordHash.isBlank()
                || attemptedUsername == null || attemptedPassword == null) {
            return false;
        }
        boolean matchesUsername = MessageDigest.isEqual(
                username.getBytes(StandardCharsets.UTF_8),
                attemptedUsername.getBytes(StandardCharsets.UTF_8));
        // Do not short circuit the password hash verification on an incorrect username.
        boolean matchesPassword = encoder.matches(attemptedPassword, passwordHash);
        return matchesUsername && matchesPassword;
    }
}
