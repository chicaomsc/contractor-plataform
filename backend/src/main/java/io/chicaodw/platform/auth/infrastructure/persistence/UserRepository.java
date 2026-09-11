package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** DT-017C — tenant-scoped lookup: never load a User by global id and then check the
     * company afterwards (that would be an IDOR read window). The one query both
     * resolves the target and enforces "belongs to my Company"; a miss is
     * indistinguishable from "wrong company" for the caller (both → 404). */
    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

    /** Scalar projection used by ActiveAccountFilter — no full entity hydration (DT-011A.10 §5). */
    @Query("SELECT new io.chicaodw.platform.auth.infrastructure.persistence.UserActiveState(u.status, u.authVersion) "
            + "FROM User u WHERE u.id = :userId")
    Optional<UserActiveState> findActiveStateById(@Param("userId") UUID userId);

    /**
     * DT-017C — atomic role change for a team member. Single-statement so {@code
     * auth_version + 1} is computed in the database (no read-modify-write, no lost
     * update under concurrent changes — DT-017C §22), and the WHERE clause is the
     * non-bypassable guard: only a MANAGER/MEMBER of the given Company is ever touched
     * (OWNER/SUPER_ADMIN can never be re-roled through team management — DT-017C §13),
     * and {@code role <> :newRole} makes a no-op change (MANAGER→MANAGER) affect zero
     * rows, so the caller skips the (unnecessary) session invalidation — DT-017C §6.
     * {@code clearAutomatically} so any entity read before this call is not left stale
     * in the persistence context.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE User u
               SET u.role = :newRole, u.authVersion = u.authVersion + 1
             WHERE u.id = :userId
               AND u.companyId = :companyId
               AND u.role IN (io.chicaodw.platform.auth.domain.UserRole.MANAGER, io.chicaodw.platform.auth.domain.UserRole.MEMBER)
               AND u.role <> :newRole
            """)
    int changeTenantMemberRole(@Param("userId") UUID userId, @Param("companyId") UUID companyId,
            @Param("newRole") UserRole newRole);

    /**
     * DT-017C — atomic soft-remove (deactivate) of a team member. Same rationale as
     * {@link #changeTenantMemberRole}: {@code auth_version + 1} in-database, WHERE-clause
     * guard so only a not-already-INACTIVE MANAGER/MEMBER of the given Company is ever
     * touched (OWNER/SUPER_ADMIN protected, DT-017C §13; already-INACTIVE → zero rows so
     * a repeat removal is idempotent and doesn't re-bump auth_version, DT-017C §11). No
     * physical delete — history/foreign keys on customers/estimates/etc. are preserved
     * (DT-017C §12).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE User u
               SET u.status = io.chicaodw.platform.auth.domain.UserStatus.INACTIVE,
                   u.authVersion = u.authVersion + 1
             WHERE u.id = :userId
               AND u.companyId = :companyId
               AND u.role IN (io.chicaodw.platform.auth.domain.UserRole.MANAGER, io.chicaodw.platform.auth.domain.UserRole.MEMBER)
               AND u.status <> io.chicaodw.platform.auth.domain.UserStatus.INACTIVE
            """)
    int deactivateTenantMember(@Param("userId") UUID userId, @Param("companyId") UUID companyId);
}
