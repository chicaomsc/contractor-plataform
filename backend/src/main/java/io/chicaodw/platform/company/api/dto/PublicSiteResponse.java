package io.chicaodw.platform.company.api.dto;

public record PublicSiteResponse(
        String slug,
        String name,
        String tradeName,
        String publicPhone,
        String whatsapp,
        String website,
        PublicSiteLocationResponse location,
        PublicSiteBrandingResponse branding
) {}
