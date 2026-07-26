package io.chicaodw.platform.admin.api.dto;

/** Response of {@code POST /admin/companies} (DT-011A.7 §9). */
public record CompanyOnboardingResponse(
        CompanySummary company,
        OwnerSummary owner,
        InviteResponse invite
) {}
