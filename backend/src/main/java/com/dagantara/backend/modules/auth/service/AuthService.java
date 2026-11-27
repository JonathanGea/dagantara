package com.dagantara.backend.modules.auth.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.auth.dto.ChangeTemporaryPasswordRequest;
import com.dagantara.backend.modules.auth.dto.LoginRequest;
import com.dagantara.backend.modules.auth.dto.LoginResponse;
import com.dagantara.backend.modules.tenants.repository.TenantRepository;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext.Scope;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.repository.AccountRepository;
import com.dagantara.backend.modules.users.repository.AccountRepository.AccountCredentials;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AccountRepository accounts;
    private final TenantRepository tenants;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;

    public AuthService(
            AccountRepository accounts,
            TenantRepository tenants,
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
        this.accounts = accounts;
        this.tenants = tenants;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = accessTokenTtl;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, Optional<Scope> scope) {
        AccountCredentials account = scope
                // Serialize token issuance with deactivation and session revocation.
                .filter(context -> context.tenantId() == null || tenants.lockActiveTenantForLogin(context.tenantId()))
                .flatMap(context -> accounts.findLogin(request.username(), context.tenantId()))
                .orElse(null);
        String passwordHash = account == null ? dummyPasswordHash : account.passwordHash();
        if (tooLongForBcrypt(request.password()) || !passwordEncoder.matches(request.password(), passwordHash)
                || account == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
        }

        AccountSession session = account.session();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(session.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sessionVersion", session.sessionVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(accessToken, "Bearer", expiresAt, session.passwordChangeRequired());
    }

    @Transactional
    public void changeTemporaryPassword(AccountSession session, ChangeTemporaryPasswordRequest request) {
        String temporaryHash = accounts.lockTemporaryPassword(session).orElseThrow(() ->
                new ApiException(HttpStatus.CONFLICT, "PASSWORD_CHANGE_NOT_REQUIRED",
                        "This session has no pending temporary password change"));
        if (tooLongForBcrypt(request.newPassword()) || passwordEncoder.matches(request.newPassword(), temporaryHash)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NEW_PASSWORD",
                    "Choose a different password of at most 72 UTF-8 bytes");
        }

        accounts.replaceTemporaryPassword(session.id(), passwordEncoder.encode(request.newPassword()));
    }

    private static boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > 72;
    }
}
