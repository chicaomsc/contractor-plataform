package io.chicaodw.platform.admin.api.dto;

import java.util.UUID;

/** The "company" block of the onboarding creation response (DT-011A.7 §9). */
public record CompanySummary(
        UUID id,
        String name,
        String slug,
        String country,
        String status
) {}
