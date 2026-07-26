package io.chicaodw.platform.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

/** List item for {@code GET /admin/companies} (DT-011A.7 §9). */
public record CompanyAdminSummary(
        UUID id,
        String name,
        String slug,
        String status,
        String ownerEmail,
        Instant createdAt
) {}
