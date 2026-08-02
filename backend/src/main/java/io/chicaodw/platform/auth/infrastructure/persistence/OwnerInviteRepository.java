package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.OwnerInvite;
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
public interface OwnerInviteRepository extends JpaRepository<OwnerInvite, UUID> {

    Optional<OwnerInvite> findByTokenHash(String tokenHash);

    List<OwnerInvite> findByUserIdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    /**
     * Atomic consumption — same pattern as {@code PasswordResetTokenRepository
     * .markUsedIfStillValid} (Sprint 11B.6D, SEC-AUTH-06): only affects a row that is
     * still unused, unrevoked and unexpired, so two simultaneous acceptances of the
     * same invite can never both succeed.
     */
    @Modifying
    @Query("""
            UPDATE OwnerInvite i SET i.usedAt = :now
            WHERE i.id = :id AND i.usedAt IS NULL AND i.revokedAt IS NULL AND i.expiresAt > :now
            """)
    int markUsedIfStillValid(@Param("id") UUID id, @Param("now") Instant now);
}
