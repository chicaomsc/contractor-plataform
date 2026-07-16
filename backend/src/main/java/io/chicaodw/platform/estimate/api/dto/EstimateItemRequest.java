package io.chicaodw.platform.estimate.api.dto;

import io.chicaodw.platform.estimate.domain.EstimateUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record EstimateItemRequest(
        UUID serviceId,
        @NotBlank                       String        description,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
        @NotNull                        EstimateUnit  unit,
        @NotNull @DecimalMin("0")       BigDecimal    unitPrice,
        @Min(0)                         Integer       displayOrder
) {}
