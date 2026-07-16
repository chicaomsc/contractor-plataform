package io.chicaodw.platform.servicecatalog.infrastructure.persistence;

import io.chicaodw.platform.servicecatalog.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByCompanyIdOrderByDisplayOrderAsc(UUID companyId);

    Optional<Service> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndSlug(UUID companyId, String slug);

    List<Service> findByCompanyIdAndActiveTrueOrderByDisplayOrderAsc(UUID companyId);
}
