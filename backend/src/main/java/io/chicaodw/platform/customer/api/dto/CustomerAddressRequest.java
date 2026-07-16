package io.chicaodw.platform.customer.api.dto;

import jakarta.validation.constraints.Size;

public record CustomerAddressRequest(
        @Size(max = 255) String street,
        @Size(max = 100) String city,
        @Size(max = 20)  String postalCode,
        @Size(max = 100) String region,
        @Size(max = 2)   String country
) {}
