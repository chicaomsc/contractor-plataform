package io.chicaodw.platform.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(

        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
        String companyName,

        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
        String slug,

        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 2, message = "Country must be an ISO 3166-1 alpha-2 code (e.g. PT)")
        String country,

        String tradeName,

        @NotBlank(message = "Owner name is required")
        String ownerName,

        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Owner email is required")
        String ownerEmail
) {}
