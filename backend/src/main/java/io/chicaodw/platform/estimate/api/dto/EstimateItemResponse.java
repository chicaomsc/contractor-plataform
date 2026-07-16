package io.chicaodw.platform.estimate.api.dto;

import io.chicaodw.platform.estimate.domain.EstimateUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record EstimateItemResponse(
        UUID          id,
        UUID          serviceId,
        String        description,
        BigDecimal    quantity,
        EstimateUnit  unit,
        BigDecimal    unitPrice,
        BigDecimal    total,
        int           displayOrder
) {}
