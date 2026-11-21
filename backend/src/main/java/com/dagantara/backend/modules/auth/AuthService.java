package com.dagantara.backend.modules.auth;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.common.security.BootstrapCredentials;
import com.dagantara.backend.common.security.UserIdentity;
import com.dagantara.backend.modules.users.UserAccount;
import com.dagantara.backend.modules.users.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final BootstrapCredentials bootstrapCredentials;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;

    public AuthService(
            BootstrapCredentials bootstrapCredentials,
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.issuer:dagantara-backend}") String issuer,
            @Value("${app.security.jwt.audience:dagantara-api}") String audience,
            @Value("${app.security.jwt.access-token-ttl:PT30M}") Duration accessTokenTtl
    ) {
        if (accessTokenTtl.compareTo(Duration.ofMinutes(1)) < 0
                || accessTokenTtl.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalStateException("JWT access token TTL must be 1 minute to 24 hours");
        }
        this.bootstrapCredentials = bootstrapCredentials;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = accessTokenTtl;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }
        if (request.tenantCode() == null
                && bootstrapCredentials.matches(request.username(), request.password())) {
            return issueToken(request.username(), false);
        }

        Optional<UserAccount> account = request.tenantCode() == null
                ? users.findByUsernameAndTenantIdIsNull(request.username())
                : users.findForTenantLogin(request.username(), request.tenantCode());
        String passwordHash = account.map(UserAccount::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (account.isPresent()) {
            UserAccount user = account.get();
            if (passwordMatches && UserAccount.ACTIVE.equals(user.getStatus())) {
                return issueToken(user.getId().toString(), true);
            }
        }
        throw invalidCredentials();
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    private LoginResponse issueToken(String subject, boolean persistedUser) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString());
        if (persistedUser) {
            claims.claim(UserIdentity.CLAIM, UserIdentity.USER);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()))
                .getTokenValue();
        return new LoginResponse(accessToken, "Bearer", expiresAt);
    }
}
