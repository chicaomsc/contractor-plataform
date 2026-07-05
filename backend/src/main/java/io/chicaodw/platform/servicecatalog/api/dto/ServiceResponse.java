package io.chicaodw.platform.servicecatalog.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceResponse(
        UUID    id,
        UUID    companyId,
        String  name,
        String  slug,
        String  shortDescription,
        String  description,
        String  icon,
        int     displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
