package io.chicaodw.platform.company.api.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID   id,
        String name,
        String tradeName,
        String slug,
        String email,
        String phone,
        String whatsapp,
        String website,
        String taxNumber,
        String country,
        AddressResponse address,
        String status
) {}
