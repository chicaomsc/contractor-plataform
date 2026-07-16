package io.chicaodw.platform.customer.api.dto;

public record CustomerAddressResponse(
        String street,
        String city,
        String postalCode,
        String region,
        String country
) {}
