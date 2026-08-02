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
 * Same "hash only, raw token shown once" pattern as {@code OwnerInvite}/{@code
 * PasswordResetToken} (Sprint 11B.6A, SEC-AUTH-01) — the raw token is only ever
 * returned to the client in the response body; {@link #tokenHash} (SHA-256) is the
 * only form persisted.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;
}
