package io.chicaodw.platform.company.api.dto;

public record AddressResponse(
        String street,
        String city,
        String postalCode,
        String region,
        String country
) {}
