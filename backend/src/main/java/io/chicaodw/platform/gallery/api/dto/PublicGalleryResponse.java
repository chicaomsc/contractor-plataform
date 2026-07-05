package io.chicaodw.platform.gallery.api.dto;

import java.util.UUID;

public record PublicGalleryResponse(
        UUID    id,
        String  title,
        String  description,
        String  beforeImageUrl,
        String  afterImageUrl,
        int     displayOrder,
        boolean featured
) {}
