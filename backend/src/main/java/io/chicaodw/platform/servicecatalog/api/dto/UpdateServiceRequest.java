package io.chicaodw.platform.servicecatalog.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateServiceRequest(
        @Size(min = 1, max = 255) String  name,
        @Size(max = 500)          String  shortDescription,
                                  String  description,
        @Size(max = 100)          String  icon,
        @Min(0)                   Integer displayOrder,
                                  Boolean active
) {}
