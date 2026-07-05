package io.chicaodw.platform.auth.api.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user,
        CompanyResponse company
) {}
