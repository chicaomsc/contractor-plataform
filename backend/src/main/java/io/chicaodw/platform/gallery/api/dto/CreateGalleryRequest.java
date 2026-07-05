package io.chicaodw.platform.gallery.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGalleryRequest(
        @NotBlank @Size(max = 255) String  title,
                                   String  description,
        @Min(0)                    Integer displayOrder,
                                   Boolean featured,
                                   Boolean active
) {}
