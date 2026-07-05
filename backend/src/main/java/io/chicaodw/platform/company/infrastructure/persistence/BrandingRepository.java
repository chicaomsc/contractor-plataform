package io.chicaodw.platform.company.infrastructure.persistence;

import io.chicaodw.platform.company.domain.Branding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandingRepository extends JpaRepository<Branding, UUID> {

    Optional<Branding> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
