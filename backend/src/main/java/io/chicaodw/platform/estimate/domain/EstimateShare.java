package io.chicaodw.platform.estimate.domain;

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
 * A revocable, expiring, unauthenticated share link for a single estimate. Only
 * {@link #tokenHash} is persisted — the raw token is generated and returned to the
 * owner once, at creation, and is unrecoverable afterwards (see EstimateShareService).
 */
@Entity
@Table(name = "estimate_shares")
@Getter
@Setter
@NoArgsConstructor
public class EstimateShare extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "estimate_id", nullable = false, updatable = false)
    private UUID estimateId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_access_at")
    private Instant lastAccessAt;

    @Column(name = "access_count", nullable = false)
    private long accessCount = 0;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public void recordAccess() {
        this.lastAccessAt = Instant.now();
        this.accessCount++;
    }
}
