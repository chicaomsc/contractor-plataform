package io.chicaodw.platform.auth.infrastructure.security;

import io.chicaodw.platform.auth.domain.UserRole;

import java.util.UUID;

/**
 * Lightweight principal derived from JWT claims — avoids a DB lookup on every request.
 * Stored as Authentication.principal after token validation in JwtAuthenticationFilter.
 *
 * {@code companyId} is null for a SUPER_ADMIN (no company). This principal only
 * carries identity/context from the token's signature — it is never treated as the
 * source of truth for whether the user/company are still active; that is checked
 * per-request by ActiveAccountFilter (see DT-011A.7 §14).
 */
public record JwtPrincipal(
        UUID userId,
        UUID companyId,
        String email,
        UserRole role
) {}
