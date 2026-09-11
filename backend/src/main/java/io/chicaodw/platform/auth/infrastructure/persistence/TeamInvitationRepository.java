package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.TeamInvitation;
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
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, UUID> {

    Optional<TeamInvitation> findByTokenHash(String tokenHash);

    List<TeamInvitation> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    /** DT-018A — "has invited at least one collaborator" for the onboarding checklist,
     * regardless of status (PENDING/used/revoked/expired all count — the act of inviting
     * is the signal, not whether it was ever accepted). EXISTS, not a list load / COUNT. */
    boolean existsByCompanyId(UUID companyId);

    /** Duplicate-invitation check (DT-017B §5) — a still-valid, not-yet-consumed
     * invitation for the same Company + email blocks a new {@code POST /team/invitations}
     * (the client must use resend/revoke instead of implicitly creating a second one).
     * Case-insensitive, same reasoning as {@code UserRepository.findByEmailIgnoreCase}. */
    Optional<TeamInvitation> findByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
            UUID companyId, String email, Instant now);

    /**
     * Atomic consumption — same pattern as {@code OwnerInviteRepository
     * .markUsedIfStillValid}/{@code PasswordResetTokenRepository.markUsedIfStillValid}
     * (Sprint 11B.6D, SEC-AUTH-06/14): only affects a row that is still unused,
     * unrevoked and unexpired, so two concurrent acceptances of the same token can
     * never both succeed — at most one caller ever observes this return 1 (DT-017B §16).
     */
    @Modifying
    @Query("""
            UPDATE TeamInvitation i SET i.usedAt = :now
            WHERE i.id = :id AND i.usedAt IS NULL AND i.revokedAt IS NULL AND i.expiresAt > :now
            """)
    int markUsedIfStillValid(@Param("id") UUID id, @Param("now") Instant now);
}
