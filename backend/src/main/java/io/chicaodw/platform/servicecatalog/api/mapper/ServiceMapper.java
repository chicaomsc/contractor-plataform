package io.chicaodw.platform.servicecatalog.api.mapper;

import io.chicaodw.platform.servicecatalog.api.dto.PublicServiceResponse;
import io.chicaodw.platform.servicecatalog.api.dto.ServiceResponse;
import io.chicaodw.platform.servicecatalog.domain.Service;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    ServiceResponse toResponse(Service service);

    PublicServiceResponse toPublicResponse(Service service);
}
