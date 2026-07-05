package io.chicaodw.platform.servicecatalog.api.dto;

import java.util.UUID;

public record PublicServiceResponse(
        UUID   id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String icon,
        int    displayOrder
) {}
