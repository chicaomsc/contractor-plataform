package io.chicaodw.platform.company.infrastructure.persistence;

import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Single-column scalar lookup used by ActiveAccountFilter — no full entity hydration. */
    @Query("SELECT c.status FROM Company c WHERE c.id = :companyId")
    Optional<CompanyStatus> findStatusById(@Param("companyId") UUID companyId);

    // The explicit CAST(:search AS string) is load-bearing, not decorative: with a
    // NULL :search parameter (the "no filter" case, i.e. plain GET /admin/companies),
    // Postgres's JDBC driver cannot infer a type for a NULL parameter nested inside
    // CONCAT/LOWER and defaults to `bytea`, which then fails with "function
    // lower(bytea) does not exist". Casting forces `text`, which resolves cleanly for
    // both the NULL and the real-search-string case.
    @Query("""
            SELECT c FROM Company c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:search IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(c.slug) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Company> search(@Param("status") CompanyStatus status, @Param("search") String search, Pageable pageable);
}
