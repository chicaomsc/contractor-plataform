package io.chicaodw.platform.estimate.api.dto;

import io.chicaodw.platform.estimate.domain.EstimateUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record MaterialResponse(
        UUID          id,
        String        name,
        String        description,
        BigDecimal    quantity,
        EstimateUnit  unit,
        BigDecimal    unitPrice,
        BigDecimal    total,
        int           displayOrder
) {}
