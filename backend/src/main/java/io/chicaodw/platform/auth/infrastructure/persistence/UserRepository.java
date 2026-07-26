package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByCompanyId(UUID companyId);

    List<User> findByCompanyIdIn(List<UUID> companyIds);

    /** Single-column scalar lookup used by ActiveAccountFilter — no full entity hydration. */
    @Query("SELECT u.status FROM User u WHERE u.id = :userId")
    Optional<UserStatus> findStatusById(@Param("userId") UUID userId);
}
