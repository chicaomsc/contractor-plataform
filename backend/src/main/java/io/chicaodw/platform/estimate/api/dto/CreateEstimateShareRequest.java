package io.chicaodw.platform.estimate.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** {@code expiresInDays} is optional — defaults to 30 (see EstimateShareService). Never infinite. */
public record CreateEstimateShareRequest(
        @Min(1) @Max(365) Integer expiresInDays
) {}
