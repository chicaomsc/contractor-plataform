package io.chicaodw.platform.estimate.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full replace-on-update for {@code items}/{@code materials}: when provided (non-null),
 * the whole collection is replaced. Scalar fields follow the codebase's usual
 * null-means-unchanged convention. Only allowed while the estimate is in DRAFT.
 */
public record UpdateEstimateRequest(
                                        UUID    customerId,
        @Size(min = 1, max = 255)      String  title,
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
