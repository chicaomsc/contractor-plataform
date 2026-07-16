package io.chicaodw.platform.estimate.infrastructure.persistence;

import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstimateRepository extends JpaRepository<Estimate, UUID> {

    Optional<Estimate> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndCustomerId(UUID companyId, UUID customerId);

    /** Fetch-joins items to avoid N+1 on the detail endpoint; materials load lazily (still a single extra query). */
    @Query("""
            select distinct e from Estimate e
            left join fetch e.items
            where e.id = :id and e.companyId = :companyId
            """)
    Optional<Estimate> findDetailByIdAndCompanyId(@Param("id") UUID id, @Param("companyId") UUID companyId);

    @Query("""
            select e from Estimate e
            where e.companyId = :companyId
            and (:status is null or e.status = :status)
            and (:customerId is null or e.customerId = :customerId)
            order by e.issueDate desc, e.createdAt desc
            """)
    List<Estimate> findByFilters(
            @Param("companyId") UUID companyId,
            @Param("status") EstimateStatus status,
            @Param("customerId") UUID customerId);
}
