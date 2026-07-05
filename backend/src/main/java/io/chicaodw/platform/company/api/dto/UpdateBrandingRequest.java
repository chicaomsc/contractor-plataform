package io.chicaodw.platform.company.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBrandingRequest(
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a valid hex colour, e.g. #1E40AF")
        String primaryColor,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a valid hex colour, e.g. #3B82F6")
        String secondaryColor,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a valid hex colour, e.g. #F59E0B")
        String accentColor,

        @Size(max = 500)  String tagline,
        @Size(max = 2000) String aboutText,
        @Size(max = 2000) String footerText,
        @Size(max = 20)   String quotationPrefix,
        @Size(max = 255)  String signatureName
) {}
