package io.chicaodw.platform.admin.api.dto;

/** Response of {@code POST /admin/companies/{companyId}/owners} (DT-011A.7 §9). */
public record OwnerInviteResponse(
        OwnerSummary owner,
        InviteResponse invite
) {}
