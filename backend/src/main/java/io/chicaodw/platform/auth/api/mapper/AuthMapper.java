package io.chicaodw.platform.auth.api.mapper;

import io.chicaodw.platform.auth.api.dto.BrandingResponse;
import io.chicaodw.platform.auth.api.dto.CompanyResponse;
import io.chicaodw.platform.auth.api.dto.SettingsResponse;
import io.chicaodw.platform.auth.api.dto.UserResponse;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "role",   expression = "java(user.getRole().name())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserResponse toUserResponse(User user);

    @Mapping(target = "status", expression = "java(company.getStatus().name())")
    CompanyResponse toCompanyResponse(Company company);

    BrandingResponse toBrandingResponse(Branding branding);

    SettingsResponse toSettingsResponse(Settings settings);
}
