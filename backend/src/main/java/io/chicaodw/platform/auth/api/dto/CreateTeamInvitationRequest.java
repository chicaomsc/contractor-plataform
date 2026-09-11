package io.chicaodw.platform.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * {@code role} is a raw string here, not the {@code UserRole} enum directly — Jackson
 * would otherwise accept {@code "SUPER_ADMIN"}/{@code "OWNER"} as syntactically valid
 * JSON and only fail deserialization with a generic 400, instead of the specific,
 * intentional business error {@code TeamInvitationService} produces for those. The
 * MANAGER/MEMBER-only rule is enforced in the service (DT-017B §3), not here.
 */
public record CreateTeamInvitationRequest(

        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Role is required")
        String role
) {}
