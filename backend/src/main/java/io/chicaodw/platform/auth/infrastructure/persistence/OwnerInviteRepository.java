package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.OwnerInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerInviteRepository extends JpaRepository<OwnerInvite, UUID> {

    Optional<OwnerInvite> findByTokenHash(String tokenHash);

    List<OwnerInvite> findByUserIdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);
}
