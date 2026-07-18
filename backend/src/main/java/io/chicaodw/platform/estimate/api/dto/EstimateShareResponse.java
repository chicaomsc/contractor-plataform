package io.chicaodw.platform.estimate.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code token} is only ever populated in the response immediately following creation
 * — only {@code tokenHash} is persisted, so it cannot be recovered afterwards. A later
 * {@code GET} returns every other field (to show link status) with {@code token} null.
 */
public record EstimateShareResponse(
        UUID id,
        String status,
        String token,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastAccessAt,
        long accessCount
) {}
