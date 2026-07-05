package io.chicaodw.platform.auth.infrastructure.security;

import io.chicaodw.platform.auth.domain.UserRole;

import java.util.UUID;

/**
 * Lightweight principal derived from JWT claims — avoids a DB lookup on every request.
 * Stored as Authentication.principal after token validation in JwtAuthenticationFilter.
 */
public record JwtPrincipal(
        UUID userId,
        UUID companyId,
        String email,
        UserRole role
) {}
