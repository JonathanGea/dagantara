package com.dagantara.backend.modules.users.service;

import com.dagantara.backend.modules.users.repository.AccountRepository;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlatformAdminBootstrap implements ApplicationRunner {
    private static final Pattern BCRYPT_HASH =
            Pattern.compile("^\\$2[aby]\\$12\\$[./A-Za-z0-9]{53}$");

    private final AccountRepository accounts;
    private final String username;
    private final String passwordHash;

    public PlatformAdminBootstrap(
            AccountRepository accounts,
            @Value("${app.security.bootstrap.username:}") String username,
            @Value("${app.security.bootstrap.password-hash:}") String passwordHash
    ) {
        this.accounts = accounts;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isEmpty() && passwordHash.isEmpty()) {
            return;
        }
        if (username.isBlank() || username.length() > 128 || !BCRYPT_HASH.matcher(passwordHash).matches()) {
            throw new IllegalStateException("Admin setup requires a username and a BCrypt cost-12 hash");
        }

        // All concurrent setup attempts lock the same seeded role before checking for the account.
        UUID roleId = accounts.lockPlatformAdminRole();
        accounts.insertInitialAdminIfAbsent(roleId, username, passwordHash);
    }
}
