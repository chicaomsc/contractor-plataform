package io.chicaodw.platform.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCompanyStatusRequest(

        @NotBlank(message = "Status is required")
        String status
) {}
