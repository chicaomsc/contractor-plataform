package io.chicaodw.platform.servicecatalog.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReorderRequest(
        @NotNull @Min(0) Integer displayOrder
) {}
