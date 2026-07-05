package io.chicaodw.platform.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Owner name is required")
        String ownerName,

        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
        String companyName,

        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 2, message = "Country must be an ISO 3166-1 alpha-2 code (e.g. PT)")
        String country
) {}
