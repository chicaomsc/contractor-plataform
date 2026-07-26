package io.chicaodw.platform.admin.api.dto;

import java.time.Instant;

/** The raw invite token, shown exactly once (DT-011A.7 §9/§11). */
public record InviteResponse(
        String token,
        Instant expiresAt
) {}
