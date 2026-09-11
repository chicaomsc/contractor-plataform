package io.chicaodw.platform.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe projection of a {@code TeamInvitation} — deliberately never carries the raw
 * token or {@code tokenHash} (DT-017B §12/§23); the token is delivered exclusively via
 * the invitation email. {@code status} is derived (PENDING/EXPIRED/USED/REVOKED), not
 * a persisted column — see {@code TeamInvitationService}.
 */
public record TeamInvitationResponse(
        UUID id,
        String email,
        String role,
        String status,
        Instant expiresAt,
        Instant createdAt
) {}
