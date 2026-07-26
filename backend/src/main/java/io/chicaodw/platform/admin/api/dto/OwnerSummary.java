package io.chicaodw.platform.admin.api.dto;

import java.util.UUID;

/** The "owner" block of the onboarding/invite-owner creation responses (DT-011A.7 §9). */
public record OwnerSummary(
        UUID id,
        String email,
        String name,
        String status
) {}
