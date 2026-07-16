package io.chicaodw.platform.estimate.application;

import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

/**
 * Generates human-readable, per-company sequential estimate numbers (e.g. {@code ORC-2026-0001}).
 *
 * Concurrency safety: the sequence counter lives in {@code estimate_number_sequences}
 * (PK {@code company_id, year}) and is advanced with a single atomic
 * {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING} statement. PostgreSQL takes the
 * row lock as part of that statement, so two concurrent requests for the same company/year
 * serialize on that row instead of racing on a read-then-write — no gaps, no duplicates,
 * no explicit application-level locking. See ADR-007.
 *
 * Runs in the caller's transaction (estimate creation) rather than REQUIRES_NEW: if the
 * enclosing create fails and rolls back, the sequence value rolls back with it and is
 * reused by the next attempt, so numbers stay dense even under failed creations.
 */
@Component
@RequiredArgsConstructor
public class EstimateNumberGenerator {

    private static final String DEFAULT_PREFIX = "ORC";

    @PersistenceContext
    private final EntityManager entityManager;

    private final BrandingRepository brandingRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public String generate(UUID companyId) {
        int year = Year.now().getValue();
        int sequence = nextSequenceValue(companyId, year);
        String prefix = resolvePrefix(companyId);
        return "%s-%d-%04d".formatted(prefix, year, sequence);
    }

    private int nextSequenceValue(UUID companyId, int year) {
        Object result = entityManager.createNativeQuery("""
                        INSERT INTO estimate_number_sequences (company_id, year, last_value)
                        VALUES (:companyId, :year, 1)
                        ON CONFLICT (company_id, year)
                        DO UPDATE SET last_value = estimate_number_sequences.last_value + 1
                        RETURNING last_value
                        """)
                .setParameter("companyId", companyId)
                .setParameter("year", year)
                .getSingleResult();
        return ((Number) result).intValue();
    }

    private String resolvePrefix(UUID companyId) {
        return brandingRepository.findByCompanyId(companyId)
                .map(branding -> branding.getQuotationPrefix())
                .filter(prefix -> prefix != null && !prefix.isBlank())
                .orElse(DEFAULT_PREFIX);
    }
}
