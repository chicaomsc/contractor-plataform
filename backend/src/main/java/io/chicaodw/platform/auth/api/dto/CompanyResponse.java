package io.chicaodw.platform.auth.api.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String slug,
        String email,
        String country,
        String status
) {}
