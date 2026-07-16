package io.chicaodw.platform.customer.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255)    String email,
        @Size(max = 50)            String phone,
        @Size(max = 50)            String taxNumber,
        @Valid                     CustomerAddressRequest address,
        @Size(max = 2000)          String notes
) {}
