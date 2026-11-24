package com.dagantara.backend.modules.tenants.service;

import com.dagantara.backend.modules.tenants.repository.TenantRepository;
import com.dagantara.backend.modules.tenants.service.TenantLoginAddress.RequestPath;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TenantRequestContext {
    private final TenantLoginAddress addresses;
    private final TenantRepository tenants;

    public TenantRequestContext(TenantLoginAddress addresses, TenantRepository tenants) {
        this.addresses = addresses;
        this.tenants = tenants;
    }

    public Optional<Scope> resolve(HttpServletRequest request) {
        String hostname = TenantLoginAddress.normalizeHost(request.getServerName());
        RequestPath path = requestPath(request);
        if (addresses.isPlatformHost(hostname) && path.shopPath().isEmpty()) {
            return Optional.of(new Scope(null, path.apiPath()));
        }
        return tenants.findTenantAt(hostname, path.shopPath())
                .map(tenantId -> new Scope(tenantId, path.apiPath()));
    }

    public Optional<UUID> resolveActiveTenant(HttpServletRequest request) {
        String hostname = TenantLoginAddress.normalizeHost(request.getServerName());
        return tenants.findActiveTenantAt(hostname, requestPath(request).shopPath());
    }

    private RequestPath requestPath(HttpServletRequest request) {
        return addresses.splitRequestPath(request.getRequestURI().substring(request.getContextPath().length()));
    }

    public record Scope(UUID tenantId, String apiPath) {
    }
}
