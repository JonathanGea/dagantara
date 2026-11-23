package com.dagantara.backend.common.security;

import com.dagantara.backend.modules.tenants.service.TenantRequestContext;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext.Scope;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountSessionFilter extends OncePerRequestFilter {
    public static final String ACCOUNT_ATTRIBUTE = "authenticatedAccount";

    private final AccountRepository accounts;
    private final TenantRequestContext contexts;
    private final SecurityErrorHandler errors;

    public AccountSessionFilter(AccountRepository accounts, TenantRequestContext contexts,
                                SecurityErrorHandler errors) {
        this.accounts = accounts;
        this.contexts = contexts;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<AccountSession> account = accountId(auth.getToken().getSubject()).flatMap(accounts::findSession);
        if (account.isEmpty() || !currentVersion(auth, account.get())) {
            errors.commence(request, response, new BadCredentialsException("Invalid session"));
            return;
        }

        AccountSession session = account.get();
        Optional<Scope> scope = contexts.resolve(request)
                .filter(context -> Objects.equals(context.tenantId(), session.tenantId()));
        if (scope.isEmpty()) {
            errors.handle(request, response, new AccessDeniedException("Operation outside session scope"));
            return;
        }

        if (session.passwordChangeRequired() && !isPasswordChange(request, scope.get())) {
            errors.passwordChangeRequired(request, response);
            return;
        }

        request.setAttribute(ACCOUNT_ATTRIBUTE, session);
        chain.doFilter(request, response);
    }

    private static Optional<UUID> accountId(String subject) {
        try {
            return subject == null ? Optional.empty() : Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static boolean currentVersion(JwtAuthenticationToken auth, AccountSession session) {
        Object version = auth.getToken().getClaims().get("sessionVersion");
        if (version instanceof Number number) {
            return number.longValue() == session.sessionVersion();
        }
        // Tokens issued before tenant onboarding exist only for platform accounts.
        return version == null && session.tenantId() == null && session.sessionVersion() == 0;
    }

    private static boolean isPasswordChange(HttpServletRequest request, Scope scope) {
        return HttpMethod.POST.matches(request.getMethod()) && "/api/v1/auth/password".equals(scope.apiPath());
    }
}
