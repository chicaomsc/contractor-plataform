package io.chicaodw.platform.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe projection of a {@code User} for {@code GET /team/members} (DT-017C). Carries
 * only what a team-management screen needs — never {@code passwordHash}, {@code
 * authVersion}, or anything about refresh tokens / session internals.
 */
public record TeamMemberResponse(
        UUID id,
        String name,
        String email,
        String role,
        String status,
        Instant createdAt
) {}
