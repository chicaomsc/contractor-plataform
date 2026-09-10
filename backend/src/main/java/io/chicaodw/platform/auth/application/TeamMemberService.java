package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.api.dto.TeamMemberResponse;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * DT-017C — management of the Users that already belong to a Company (the "team"): list
 * them, change a MANAGER/MEMBER's role, or soft-remove (deactivate) one. OWNER-only —
 * every entry point is reached only through {@code TeamMemberController}
 * ({@code @PreAuthorize("hasRole('OWNER')")}).
 *
 * <p>There is no Membership entity: a User still belongs directly to one Company via
 * {@code User.companyId}. "Team members" are simply the Users with that companyId.
 *
 * <p><b>OWNER is protected here, in the service, not just in the UI</b> (DT-017C §13):
 * neither role change nor removal can ever touch a User whose role is OWNER (or
 * SUPER_ADMIN) — enforced by an explicit check for a clear error message AND by the
 * WHERE clause of the atomic UPDATE queries as the non-bypassable guarantee. The caller
 * is always an OWNER, so "cannot act on self" is subsumed by "cannot act on any OWNER".
 * Ownership provisioning/transfer stays entirely with the platform-admin flow.
 *
 * <p>A real role change or a removal invalidates the target's existing sessions
 * immediately (DT-017C §7/§10): the atomic UPDATE bumps {@code auth_version} (so
 * {@code ActiveAccountFilter} rejects the old access token on the very next request,
 * DT-014) and this service then revokes every refresh token for that user — both inside
 * one {@code @Transactional} method, so they commit together or not at all.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TeamMemberService {

    private static final Map<UserRole, Integer> ROLE_LIST_ORDER =
            Map.of(UserRole.OWNER, 0, UserRole.MANAGER, 1, UserRole.MEMBER, 2);

    private static final String INVALID_ROLE_MESSAGE = "Role must be MANAGER or MEMBER";
    private static final String OWNER_ROLE_CHANGE_MESSAGE =
            "An OWNER's role cannot be changed through team management.";
    private static final String OWNER_REMOVAL_MESSAGE =
            "An OWNER cannot be removed through team management.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // ── GET /team/members ──────────────────────────────────────────────────────

    /**
     * Every tenant User of the Company, any status (ACTIVE/INACTIVE/PENDING — a removed
     * member stays visible as INACTIVE, DT-017C §12). SUPER_ADMIN never has a companyId
     * so never appears; the filter is only belt-and-suspenders. Deterministic order:
     * OWNER, then MANAGER, then MEMBER; within a group, by name then email
     * (case-insensitive) — same "sort in Java on a small per-company list" style
     * AdminCompanyService already uses for owners.
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listMembers(UUID companyId) {
        return userRepository.findByCompanyId(companyId).stream()
                .filter(u -> u.getRole() != UserRole.SUPER_ADMIN)
                .sorted(Comparator
                        .comparingInt((User u) -> ROLE_LIST_ORDER.getOrDefault(u.getRole(), Integer.MAX_VALUE))
                        .thenComparing(u -> u.getName() == null ? "" : u.getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    // ── PATCH /team/members/{userId}/role ──────────────────────────────────────

    public TeamMemberResponse changeRole(UUID companyId, UUID targetUserId, String rawRole) {
        UserRole newRole = parseAssignableRole(rawRole);
        User target = requireTenantMember(companyId, targetUserId);
        if (isOwnerRole(target.getRole())) {
            throw new ConflictException(OWNER_ROLE_CHANGE_MESSAGE);
        }

        int updated = userRepository.changeTenantMemberRole(targetUserId, companyId, newRole);
        if (updated > 0) {
            // A real change happened → the target's old access tokens must stop being
            // accepted (auth_version was bumped by the UPDATE above) and its refresh
            // tokens must stop working. Same transaction — all-or-nothing (DT-017C §8).
            refreshTokenRepository.revokeAllForUser(targetUserId);
            return responseWithRole(target, newRole);
        }
        // updated == 0 → the role already equalled newRole (idempotent, DT-017C §6) or a
        // concurrent change got there first; either way nothing to invalidate.
        return toResponse(target);
    }

    // ── DELETE /team/members/{userId} ──────────────────────────────────────────

    /**
     * Soft-remove: {@code status → INACTIVE}, never a physical delete (DT-017C §9/§12).
     * Idempotent for an already-INACTIVE member — a second call is a no-op 204 and does
     * NOT re-bump auth_version / re-revoke (DT-017C §11).
     */
    public void removeMember(UUID companyId, UUID targetUserId) {
        User target = requireTenantMember(companyId, targetUserId);
        if (isOwnerRole(target.getRole())) {
            throw new ConflictException(OWNER_REMOVAL_MESSAGE);
        }

        int updated = userRepository.deactivateTenantMember(targetUserId, companyId);
        if (updated > 0) {
            refreshTokenRepository.revokeAllForUser(targetUserId);
        }
        // updated == 0 → already INACTIVE (or a concurrent removal won) → idempotent no-op.
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User requireTenantMember(UUID companyId, UUID targetUserId) {
        // Tenant-scoped lookup (DT-017C §19): a wrong-company / unknown id both surface
        // as 404 — the target's name/email/role/status of another Company is never
        // revealed (DT-017C §15).
        return userRepository.findByIdAndCompanyId(targetUserId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", targetUserId));
    }

    private boolean isOwnerRole(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.SUPER_ADMIN;
    }

    private UserRole parseAssignableRole(String rawRole) {
        UserRole parsed;
        try {
            parsed = UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(INVALID_ROLE_MESSAGE);
        }
        if (parsed != UserRole.MANAGER && parsed != UserRole.MEMBER) {
            throw new BusinessRuleException(INVALID_ROLE_MESSAGE);
        }
        return parsed;
    }

    private TeamMemberResponse toResponse(User user) {
        return new TeamMemberResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt());
    }

    /** Response after a successful role change — the atomic UPDATE bypassed the
     * persistence context, so {@code target} still shows the old role; every other
     * field on it is unchanged, so only {@code role} needs the new value spliced in. */
    private TeamMemberResponse responseWithRole(User target, UserRole newRole) {
        return new TeamMemberResponse(
                target.getId(),
                target.getName(),
                target.getEmail(),
                newRole.name(),
                target.getStatus().name(),
                target.getCreatedAt());
    }
}
