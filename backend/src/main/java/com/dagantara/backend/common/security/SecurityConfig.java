package com.dagantara.backend.common.security;

import com.dagantara.backend.modules.tenants.service.TenantRequestContext;
import com.dagantara.backend.modules.users.repository.AccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_API_PRODUCTS;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_PAGE_PRODUCTS;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_PAGE_PRODUCT_ID;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_SHOP;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityErrorHandler errorHandler,
                                    AccountRepository accounts, TenantRequestContext contexts)
            throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        var storefront = new OrRequestMatcher(
                paths.matcher(HttpMethod.GET, STOREFRONT_API_PRODUCTS),
                paths.matcher(HttpMethod.GET, STOREFRONT_API_PRODUCTS + "/{productId}"),
                paths.matcher(HttpMethod.GET, STOREFRONT_SHOP + STOREFRONT_API_PRODUCTS),
                paths.matcher(HttpMethod.GET, STOREFRONT_SHOP + STOREFRONT_API_PRODUCTS + "/{productId}"),
                paths.matcher(HttpMethod.GET, STOREFRONT_PAGE_PRODUCTS),
                paths.matcher(HttpMethod.GET, STOREFRONT_PAGE_PRODUCTS + STOREFRONT_PAGE_PRODUCT_ID),
                paths.matcher(HttpMethod.GET, STOREFRONT_SHOP + STOREFRONT_PAGE_PRODUCTS),
                paths.matcher(HttpMethod.GET, STOREFRONT_SHOP + STOREFRONT_PAGE_PRODUCTS + STOREFRONT_PAGE_PRODUCT_ID));
        var bearer = new DefaultBearerTokenResolver();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login",
                                "/*/api/v1/auth/login", "/api/v1/tenant-applications").permitAll()
                        .requestMatchers(storefront).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(request -> storefront.matches(request) ? null : bearer.resolve(request))
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        http.addFilterAfter(new AccountSessionFilter(accounts, contexts, errorHandler),
                BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
