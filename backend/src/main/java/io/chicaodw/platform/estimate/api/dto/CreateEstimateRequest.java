package io.chicaodw.platform.estimate.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateEstimateRequest(
        @NotNull                        UUID    customerId,
        @NotBlank @Size(max = 255)      String  title,
                                        String  description,
                                        LocalDate validUntil,
                                        LocalDate expectedStartDate,
        @Min(1)                         Integer estimatedDurationDays,
                                        String  notes,
                                        String  terms,
        @DecimalMin("0") @DecimalMax("100") BigDecimal vatRate,
        @DecimalMin("0") @DecimalMax("100") BigDecimal upfrontPercentage,
        @Valid                          List<EstimateItemRequest> items,
        @Valid                          List<MaterialRequest>     materials
) {}
