package io.chicaodw.platform.gallery.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateGalleryRequest(
        @Size(min = 1, max = 255) String  title,
                                  String  description,
        @Min(0)                   Integer displayOrder,
                                  Boolean featured,
                                  Boolean active
) {}
