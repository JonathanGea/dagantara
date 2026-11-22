package com.dagantara.backend.modules.auth.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.auth.dto.LoginRequest;
import com.dagantara.backend.modules.auth.dto.LoginResponse;
import com.dagantara.backend.modules.users.repository.PlatformAccountRepository;
import com.dagantara.backend.modules.users.repository.PlatformAccountRepository.PlatformLogin;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final PlatformAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;

    public AuthService(
            PlatformAccountRepository accounts,
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
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = accessTokenTtl;
    }

    public LoginResponse login(LoginRequest request) {
        PlatformLogin account = accounts.findLoginByUsername(request.username()).orElse(null);
        String passwordHash = account == null ? dummyPasswordHash : account.passwordHash();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.password(), passwordHash) || account == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid credentials");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(account.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new LoginResponse(accessToken, "Bearer", expiresAt);
    }
}
