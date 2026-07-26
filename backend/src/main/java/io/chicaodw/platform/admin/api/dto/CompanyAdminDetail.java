package io.chicaodw.platform.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

/** The "company" block of {@code GET /admin/companies/{id}} (DT-011A.7 §9). */
public record CompanyAdminDetail(
        UUID id,
        String name,
        String slug,
        String email,
        String country,
        String tradeName,
        String status,
        Instant createdAt
) {}
