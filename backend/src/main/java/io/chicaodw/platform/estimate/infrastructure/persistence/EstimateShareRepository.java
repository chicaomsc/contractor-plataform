package io.chicaodw.platform.estimate.infrastructure.persistence;

import io.chicaodw.platform.estimate.domain.EstimateShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstimateShareRepository extends JpaRepository<EstimateShare, UUID> {

    Optional<EstimateShare> findByTokenHash(String tokenHash);

    Optional<EstimateShare> findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(UUID estimateId, UUID companyId);

    Optional<EstimateShare> findFirstByEstimateIdAndCompanyIdOrderByCreatedAtDesc(UUID estimateId, UUID companyId);
}
