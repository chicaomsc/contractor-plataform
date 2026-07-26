package io.chicaodw.platform.auth.domain;

import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A revocable, expiring, single-use invite for a PENDING owner to set their own
 * password (see InviteService). Only {@link #tokenHash} is persisted — the raw token
 * is generated and returned to the SUPER_ADMIN once, at creation or reissue time, and
 * is unrecoverable afterwards (same "shown once" model as EstimateShare).
 */
@Entity
@Table(name = "owner_invites")
@Getter
@Setter
@NoArgsConstructor
public class OwnerInvite extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    public boolean isUsable() {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
