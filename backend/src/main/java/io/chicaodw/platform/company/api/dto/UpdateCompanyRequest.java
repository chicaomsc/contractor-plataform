package io.chicaodw.platform.company.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(min = 2, max = 255)  String name,
        @Size(max = 255)           String tradeName,
        @Email @Size(max = 255)    String email,
        @Size(max = 50)            String phone,
        @Size(max = 50)            String whatsapp,
        @Size(max = 500)           String website,
        @Size(max = 50)            String taxNumber,
        @Size(max = 2)             String country,
        @Valid                     AddressRequest address
) {}
