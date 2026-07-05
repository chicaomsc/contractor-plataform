package io.chicaodw.platform.company.api.mapper;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.company.api.dto.AddressResponse;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.CompanyResponse;
import io.chicaodw.platform.company.api.dto.SettingsResponse;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "status", expression = "java(company.getStatus().name())")
    CompanyResponse toCompanyResponse(Company company);

    AddressResponse toAddressResponse(Address address);

    BrandingResponse toBrandingResponse(Branding branding);

    SettingsResponse toSettingsResponse(Settings settings);
}
