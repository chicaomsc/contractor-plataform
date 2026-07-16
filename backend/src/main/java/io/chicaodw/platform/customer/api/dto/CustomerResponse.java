package io.chicaodw.platform.customer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID                    id,
        UUID                    companyId,
        String                  name,
        String                  email,
        String                  phone,
        String                  taxNumber,
        CustomerAddressResponse address,
        String                  notes,
        boolean                 active,
        Instant                 createdAt,
        Instant                 updatedAt
) {}
