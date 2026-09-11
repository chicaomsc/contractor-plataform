package io.chicaodw.platform.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Default (used whenever APP_CORS_ALLOWED_ORIGINS/app.cors.allowed-origins isn't
    // set by any profile/env — i.e. a bare `./mvnw spring-boot:run` per README.md, and
    // the shared integration-test context, AbstractIntegrationTest, which doesn't
    // override this either) covers both ports the frontend dev server actually runs
    // on locally: 3000 (npm run dev default) and 3001 (used when 3000 is already
    // taken, and by Playwright's E2E config, playwright.config.ts). Both localhost and
    // 127.0.0.1 for each, matching the pair already present for 3000. Production always
    // overrides this via APP_CORS_ALLOWED_ORIGINS — ProductionReadinessValidator
    // requires it non-blank and rejects a localhost/127.0.0.1 host outright when
    // profile 'prod' is active, so this default is never reachable there.
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:3001,http://127.0.0.1:3001}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthRateLimitFilter rateLimitFilter,
                                           JwtAuthenticationFilter jwtFilter,
                                           ActiveAccountFilter activeAccountFilter,
                                           JwtAuthenticationEntryPoint entryPoint) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/invites/accept",
                                "/auth/password/forgot",
                                "/auth/password/reset",
                                // DT-017B — the person accepting a team invitation has no
                                // account/session yet, same reasoning as /auth/invites/accept
                                // above. Every validation (token exists, unexpired, unused,
                                // unrevoked, Company active) still happens inside
                                // TeamInvitationService.acceptInvitation — "public" here only
                                // means "no JWT required to reach it" (DT-017B §21).
                                "/auth/team-invitations/accept",
                                "/public/**",
                                "/uploads/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Defense in depth alongside @PreAuthorize on AdminController
                        // (DT-011A.7 §6) — protects /admin/** even if a future
                        // endpoint forgets the method-security annotation.
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(activeAccountFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        // Spring's relaxed binding for a comma-separated List<String> does not trim
        // whitespace around each element, so " https://a.pt, https://b.pt" would
        // otherwise leave a literal leading space in an allowed origin.
        configuration.setAllowedOrigins(
                allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isBlank()).toList());
        configuration.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 12 (vs. BCryptPasswordEncoder's own default of 10) — SEC-AUTH-10/DT-011B.5 §9.
    // Existing hashes keep validating: BCrypt encodes its own cost factor in the hash
    // string, so a strength change only affects newly-created hashes, never existing
    // ones — see docs/operations/runbook.md for the rehash-on-login strategy this
    // enables without a bulk migration.
    @Value("${app.security.bcrypt-strength:12}")
    private int bcryptStrength;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
