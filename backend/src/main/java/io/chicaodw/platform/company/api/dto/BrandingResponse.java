package io.chicaodw.platform.company.api.dto;

import java.util.UUID;

public record BrandingResponse(
        UUID   id,
        UUID   companyId,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String tagline,
        String aboutText,
        String footerText,
        String quotationPrefix,
        String signatureName
) {}
