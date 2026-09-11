package io.chicaodw.platform.auth.api;

import io.chicaodw.platform.auth.api.dto.CreateTeamInvitationRequest;
import io.chicaodw.platform.auth.api.dto.TeamInvitationResponse;
import io.chicaodw.platform.auth.application.TeamInvitationService;
import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant team-invitation management (DT-017B) — OWNER-only (DT-017A matrix, "Equipe"
 * row: only OWNER manages the team in this first version; see SecurityConfig's default
 * for why this needs no matcher there too — every method below already requires
 * authentication via {@code @PreAuthorize}, same as every other tenant controller).
 * {@code companyId}/{@code invitedByUserId} are never accepted from the client — both
 * come exclusively from {@link JwtPrincipal} (DT-017B §7/§27).
 */
@RestController
@RequestMapping("/team/invitations")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Team Invitations", description = "OWNER-only: invite/list/revoke/resend MANAGER or MEMBER collaborators")
public class TeamInvitationController {

    private final TeamInvitationService teamInvitationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a MANAGER or MEMBER collaborator by email")
    public TeamInvitationResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateTeamInvitationRequest request) {
        return teamInvitationService.createInvitation(
                principal.companyId(), principal.userId(), request.email(), request.role());
    }

    @GetMapping
    @Operation(summary = "List all team invitations for the authenticated company")
    public List<TeamInvitationResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return teamInvitationService.listInvitations(principal.companyId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a still-pending invitation")
    public void revoke(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        teamInvitationService.revokeInvitation(principal.companyId(), id);
    }

    @PostMapping("/{id}/resend")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Revoke the current token and issue a new one (old token stops working immediately)")
    public TeamInvitationResponse resend(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return teamInvitationService.resendInvitation(principal.companyId(), id, principal.userId());
    }
}
