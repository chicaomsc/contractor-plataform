package io.chicaodw.platform.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteOwnerRequest(

        @NotBlank(message = "Owner name is required")
        String ownerName,

        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Owner email is required")
        String ownerEmail
) {}
