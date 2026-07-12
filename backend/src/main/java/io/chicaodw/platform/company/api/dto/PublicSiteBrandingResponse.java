package io.chicaodw.platform.company.api.dto;

public record PublicSiteBrandingResponse(
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String tagline,
        String aboutText,
        String footerText
) {}
