package io.chicaodw.platform.auth.api.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID companyId,
        String email,
        String name,
        String role,
        String status
) {}
