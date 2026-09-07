package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Bulk session kill used after a successful password reset (DT-011A.10 §5). */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);

    /**
     * Atomic consumption — same pattern as {@code PasswordResetTokenRepository
     * .markUsedIfStillValid}/{@code OwnerInviteRepository.markUsedIfStillValid}
     * (Sprint 11B.6D, SEC-AUTH-14): only affects a row that is still unrevoked,
     * unexpired, AND whose session hasn't exceeded its absolute lifetime yet (DT-012 —
     * {@code sessionStartedAt > sessionCutoff}, where the caller computes {@code
     * sessionCutoff = now - absoluteLifetime}). All three checks live in the same
     * single-row atomic UPDATE on purpose: two concurrent {@code /auth/refresh} calls
     * with the same token can never both rotate successfully, regardless of which
     * condition would have failed, and there is no separate read-then-decide step that
     * could open a window between "is this session still within its absolute lifetime"
     * and "consume the token" — confirms the project's already-adopted rotate-on-use
     * strategy (option B, DT-011B.5 §9) instead of leaving the race open.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.id = :id AND r.revoked = false "
            + "AND r.expiresAt > :now AND r.sessionStartedAt > :sessionCutoff")
    int markRevokedIfStillValid(@Param("id") UUID id, @Param("now") Instant now,
            @Param("sessionCutoff") Instant sessionCutoff);
}
