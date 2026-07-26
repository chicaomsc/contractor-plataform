package io.chicaodw.platform.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Owner item in {@code GET /admin/companies/{id}}'s "owners" list (DT-011A.7 §9). */
public record OwnerAdminResponse(
        UUID id,
        String email,
        String name,
        String role,
        String status,
        Instant createdAt
) {}
