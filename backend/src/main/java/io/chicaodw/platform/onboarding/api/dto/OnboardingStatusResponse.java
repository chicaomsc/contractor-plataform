package io.chicaodw.platform.onboarding.api.dto;

/**
 * DT-018A — every flag is derived live from existing entity state, never persisted.
 * {@code teamCompleted} is deliberately informational/optional: inviting a collaborator
 * is not required to finish onboarding, so no aggregate "overallCompleted" field treats
 * it (or any single step) as mandatory — the frontend decides how to weigh the six flags.
 */
public record OnboardingStatusResponse(
        boolean companyCompleted,
        boolean brandingCompleted,
        boolean servicesCompleted,
        boolean customerCompleted,
        boolean estimateCompleted,
        boolean teamCompleted
) {}
