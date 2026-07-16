package io.chicaodw.platform.estimate.api.dto;

import io.chicaodw.platform.estimate.domain.EstimateStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EstimateResponse(
        UUID           id,
        UUID           companyId,
        UUID           customerId,
        String         number,
        String         title,
        String         description,
        EstimateStatus status,
        LocalDate      issueDate,
        LocalDate      validUntil,
        LocalDate      expectedStartDate,
        Integer        estimatedDurationDays,
        String         notes,
        String         terms,
        String         currency,
        BigDecimal     vatRate,
        BigDecimal     laborSubtotal,
        BigDecimal     materialSubtotal,
        BigDecimal     subtotal,
        BigDecimal     vatAmount,
        BigDecimal     total,
        BigDecimal     upfrontPercentage,
        BigDecimal     upfrontAmount,
        BigDecimal     remainingAmount,
        List<EstimateItemResponse> items,
        List<MaterialResponse>     materials,
        Instant        createdAt,
        Instant        updatedAt
) {}
