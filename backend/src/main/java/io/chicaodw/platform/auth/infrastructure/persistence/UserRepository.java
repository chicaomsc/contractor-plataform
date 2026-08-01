package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.User;
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

    /** Case-insensitive lookup used by POST /auth/password/forgot (DT-011A.10 §2) — deliberately
     * different from findByEmail, which stays case-sensitive for register/login (pre-existing behavior). */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    List<User> findByCompanyId(UUID companyId);

    List<User> findByCompanyIdIn(List<UUID> companyIds);

    /** Scalar projection used by ActiveAccountFilter — no full entity hydration (DT-011A.10 §5). */
    @Query("SELECT new io.chicaodw.platform.auth.infrastructure.persistence.UserActiveState(u.status, u.authVersion) "
            + "FROM User u WHERE u.id = :userId")
    Optional<UserActiveState> findActiveStateById(@Param("userId") UUID userId);
}
