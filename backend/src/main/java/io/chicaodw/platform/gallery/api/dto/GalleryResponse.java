package io.chicaodw.platform.gallery.api.dto;

import java.time.Instant;
import java.util.UUID;

public record GalleryResponse(
        UUID    id,
        UUID    companyId,
        String  title,
        String  description,
        String  beforeImageUrl,
        String  afterImageUrl,
        int     displayOrder,
        boolean featured,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
