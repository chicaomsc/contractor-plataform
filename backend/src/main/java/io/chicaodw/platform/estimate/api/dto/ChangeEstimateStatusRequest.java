package io.chicaodw.platform.estimate.api.dto;

import io.chicaodw.platform.estimate.domain.EstimateStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEstimateStatusRequest(
        @NotNull EstimateStatus status
) {}
