package io.chicaodw.platform.auth.api.dto;

public record MeResponse(
        UserResponse user,
        CompanyResponse company,
        BrandingResponse branding,
        SettingsResponse settings
) {}
