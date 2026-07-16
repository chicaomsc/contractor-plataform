package io.chicaodw.platform.company.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.api.dto.SettingsResponse;
import io.chicaodw.platform.company.api.dto.UpdateSettingsRequest;
import io.chicaodw.platform.company.api.mapper.CompanyMapper;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final CompanyMapper companyMapper;

    @Transactional(readOnly = true)
    public SettingsResponse getSettings(UUID companyId) {
        return settingsRepository.findByCompanyId(companyId)
                .map(companyMapper::toSettingsResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Settings", companyId));
    }

    public SettingsResponse updateSettings(UUID companyId, UpdateSettingsRequest request) {
        var settings = settingsRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings", companyId));

        if (request.defaultCurrency()       != null) settings.setDefaultCurrency(request.defaultCurrency());
        if (request.defaultTaxRate()        != null) settings.setDefaultTaxRate(request.defaultTaxRate());
        if (request.estimateValidityDays()  != null) settings.setEstimateValidityDays(request.estimateValidityDays());
        if (request.estimateFooterText()    != null) settings.setEstimateFooterText(request.estimateFooterText());
        if (request.locale()               != null) settings.setLocale(request.locale());
        if (request.timezone()             != null) settings.setTimezone(request.timezone());
        if (request.dateFormat()           != null) settings.setDateFormat(request.dateFormat());
        if (request.numberFormat()         != null) settings.setNumberFormat(request.numberFormat());
        if (request.upfrontPercentage()    != null) settings.setUpfrontPercentage(request.upfrontPercentage());

        return companyMapper.toSettingsResponse(settingsRepository.save(settings));
    }
}
