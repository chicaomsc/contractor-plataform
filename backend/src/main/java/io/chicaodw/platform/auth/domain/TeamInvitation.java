package io.chicaodw.platform.auth.domain;

import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A revocable, expiring, single-use invitation letting an OWNER add a MANAGER or MEMBER
 * to their own Company (DT-017B) — see {@code TeamInvitationService}. Deliberately a
 * separate entity/table from {@link OwnerInvite}, not a reuse of it: this row carries
 * its own {@link #email}/{@link #role} directly because, unlike an owner invite, no
 * {@link User} exists yet when a team invitation is created — the User is only ever
 * created on a successful, atomic acceptance (never PENDING beforehand).
 *
 * Only {@link #tokenHash} (SHA-256) is persisted — same "shown once" pattern as
 * {@code OwnerInvite}/{@code PasswordResetToken}/{@code RefreshToken}.
 */
@Entity
@Table(name = "team_invitations")
@Getter
@Setter
@NoArgsConstructor
public class TeamInvitation extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(nullable = false, updatable = false)
    private String email;

    /** Only {@link UserRole#MANAGER} or {@link UserRole#MEMBER} — enforced by
     * {@code TeamInvitationService} (application layer) and by the
     * {@code chk_team_invitations_role} CHECK constraint (V16, the real,
     * non-bypassable guarantee — same two-layer pattern as
     * {@code UserRoleInvariant}/{@code chk_users_role_company_id}). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private UserRole role;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "invited_by_user_id", nullable = false, updatable = false)
    private UUID invitedByUserId;

    public boolean isUsable() {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
