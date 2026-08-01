package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    /** Most recently created token for a user, regardless of state — used for the cooldown check (DT-011A.10 §7). */
    Optional<PasswordResetToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Atomic consumption: only affects a row if it is still unused, unrevoked and
     * unexpired. Closes the race between two simultaneous completions of the same
     * token — at most one caller ever observes an affected-row count of 1 (DT-011A.10
     * §4 "Concorrência").
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.usedAt = :now
            WHERE t.id = :id AND t.usedAt IS NULL AND t.revokedAt IS NULL AND t.expiresAt > :now
            """)
    int markUsedIfStillValid(@Param("id") UUID id, @Param("now") Instant now);
}
