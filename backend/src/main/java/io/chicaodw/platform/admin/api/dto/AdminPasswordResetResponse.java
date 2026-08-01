package io.chicaodw.platform.admin.api.dto;

import java.time.Instant;

/** The one-time password-reset link, shown exactly once (DT-011A.10 §13/§15). */
public record AdminPasswordResetResponse(
        String resetLink,
        Instant expiresAt
) {}
