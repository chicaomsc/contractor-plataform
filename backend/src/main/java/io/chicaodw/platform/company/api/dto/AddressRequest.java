package io.chicaodw.platform.company.api.dto;

import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 255) String street,
        @Size(max = 100) String city,
        @Size(max = 20)  String postalCode,
        @Size(max = 100) String region,
        @Size(max = 2)   String country
) {}
