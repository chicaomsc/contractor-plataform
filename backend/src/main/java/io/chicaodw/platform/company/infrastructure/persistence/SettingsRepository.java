package io.chicaodw.platform.company.infrastructure.persistence;

import io.chicaodw.platform.company.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, UUID> {

    Optional<Settings> findByCompanyId(UUID companyId);
}
