package com.dagantara.backend.modules.tenants.service;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantLoginAddress {
    public static final String SHOP_PATH_PATTERN = "/[a-z0-9][a-z0-9-]{0,62}";
    private static final Pattern SHOP_REQUEST = Pattern.compile(
            "^(/[^/]+)(/api/v1/.*)$");

    private final String platformHost;

    public TenantLoginAddress(@Value("${app.tenant.platform-host:localhost}") String platformHost) {
        this.platformHost = parseHost(platformHost)
                .orElseThrow(() -> new IllegalStateException("APP_PLATFORM_HOST must be a hostname"));
    }

    public Optional<String> parseHost(String value) {
        try {
            URI uri = URI.create("https://" + value);
            if (uri.getHost() == null || uri.getHost().length() > 253 || uri.getPort() != -1
                    || !uri.getRawPath().isEmpty() || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return Optional.empty();
            }
            return Optional.of(normalizeHost(uri.getHost()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<Address> provisioningAddress(String value) {
        try {
            URI uri = URI.create(value);
            String hostname = uri.getHost();
            String path = uri.getRawPath();
            if (hostname == null || hostname.length() > 253 || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                return Optional.empty();
            }
            hostname = normalizeHost(hostname);
            path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            boolean validPath = isPlatformHost(hostname) ? path.matches(SHOP_PATH_PATTERN) : path.isEmpty();
            if (!validPath) {
                return Optional.empty();
            }
            return Optional.of(new Address(hostname, path, uri.toASCIIString()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    static String normalizeHost(String hostname) {
        String canonical = hostname.toLowerCase(Locale.ROOT);
        return canonical.endsWith(".") ? canonical.substring(0, canonical.length() - 1) : canonical;
    }

    public boolean isPlatformHost(String hostname) {
        return platformHost.equals(normalizeHost(hostname));
    }

    public RequestPath splitRequestPath(String path) {
        var match = SHOP_REQUEST.matcher(path);
        return match.matches() ? new RequestPath(match.group(1), match.group(2)) : new RequestPath("", path);
    }

    public record Address(String hostname, String shopPath, String loginUrl) {
    }

    public record RequestPath(String shopPath, String apiPath) {
    }
}
