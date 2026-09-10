package io.chicaodw.platform.auth.api.dto;

import io.chicaodw.platform.auth.domain.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mirrors {@code AcceptInviteRequest}'s shape, plus {@code name} — a team invitation is
 * issued for an email that has no User yet (DT-017B, unlike the owner-invite flow, whose
 * PENDING User already carries a name set at creation time), so the accepting person
 * provides their own name here. {@code email}/{@code role}/{@code companyId} are never
 * accepted from the client — they come exclusively from the persisted invitation.
 */
public record AcceptTeamInvitationRequest(

        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,

        @NotBlank(message = "Password is required")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.MESSAGE)
        String password
) {}
