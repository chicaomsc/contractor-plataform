package io.chicaodw.platform.auth.api;

import io.chicaodw.platform.auth.api.dto.TeamMemberResponse;
import io.chicaodw.platform.auth.api.dto.UpdateTeamMemberRoleRequest;
import io.chicaodw.platform.auth.application.TeamMemberService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant team-member management (DT-017C) — OWNER-only, same as
 * {@link TeamInvitationController} (DT-017A matrix, "Equipe" row). MANAGER / MEMBER /
 * SUPER_ADMIN → 403. {@code companyId} is never accepted from the client — it comes
 * exclusively from {@link JwtPrincipal}; every service call is tenant-scoped from the
 * first query. Pending invitations are NOT members — that stays on
 * {@code GET /team/invitations} (DT-017B).
 */
@RestController
@RequestMapping("/team/members")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Team Members", description = "OWNER-only: list members, change a MANAGER/MEMBER's role, remove a member")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    @GetMapping
    @Operation(summary = "List every tenant User of the authenticated company (OWNER, MANAGER, MEMBER)")
    public List<TeamMemberResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return teamMemberService.listMembers(principal.companyId());
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Change a MANAGER/MEMBER's role — invalidates that user's sessions immediately")
    public TeamMemberResponse changeRole(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTeamMemberRoleRequest request) {
        return teamMemberService.changeRole(principal.companyId(), userId, request.role());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-remove a MANAGER/MEMBER (status → INACTIVE) — invalidates that user's sessions immediately")
    public void remove(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID userId) {
        teamMemberService.removeMember(principal.companyId(), userId);
    }
}
