package com.dagantara.backend.modules.auth;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.common.security.BootstrapCredentials;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final BootstrapCredentials bootstrapCredentials;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;

    public AuthService(
            BootstrapCredentials bootstrapCredentials,
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
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = accessTokenTtl;
    }

    public LoginResponse login(LoginRequest request) {
        if (!bootstrapCredentials.matches(request.username(), request.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid credentials");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(request.username())
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
