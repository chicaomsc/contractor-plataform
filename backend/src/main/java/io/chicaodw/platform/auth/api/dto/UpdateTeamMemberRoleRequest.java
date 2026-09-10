package io.chicaodw.platform.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code role} is a raw string, not the {@code UserRole} enum — same reasoning as
 * {@code CreateTeamInvitationRequest} (DT-017B): Jackson would otherwise accept
 * {@code "OWNER"}/{@code "SUPER_ADMIN"} as syntactically valid and fail with a generic
 * 400, instead of the specific business error {@code TeamMemberService} produces. The
 * MANAGER/MEMBER-only rule is enforced in the service (DT-017C §5/§13), and
 * {@code companyId}/{@code status}/{@code authVersion}/{@code email} are deliberately
 * not fields here — they are never client-mutable.
 */
public record UpdateTeamMemberRoleRequest(

        @NotBlank(message = "Role is required")
        String role
) {}
