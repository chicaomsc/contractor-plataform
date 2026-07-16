package io.chicaodw.platform.customer.api.mapper;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.customer.api.dto.CustomerAddressResponse;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.customer.domain.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    CustomerAddressResponse toAddressResponse(Address address);
}
