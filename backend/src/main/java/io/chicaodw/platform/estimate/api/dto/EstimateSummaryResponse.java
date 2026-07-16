package io.chicaodw.platform.estimate.api.dto;

import io.chicaodw.platform.estimate.domain.EstimateStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Lightweight projection used by the list endpoint — avoids loading items/materials for every row. */
public record EstimateSummaryResponse(
        UUID           id,
        UUID           companyId,
        UUID           customerId,
        String         number,
        String         title,
        EstimateStatus status,
        LocalDate      issueDate,
        LocalDate      validUntil,
        String         currency,
        BigDecimal     total,
        BigDecimal     upfrontAmount,
        BigDecimal     remainingAmount,
        Instant        createdAt,
        Instant        updatedAt
) {}
