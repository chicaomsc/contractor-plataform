package io.chicaodw.platform.company.application;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final BrandingRepository brandingRepository;
    private final SettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public Company findById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    @Transactional(readOnly = true)
    public Company findBySlug(String slug) {
        return companyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Company", slug));
    }

    public Company create(Company company) {
        if (companyRepository.existsBySlug(company.getSlug())) {
            throw new BusinessRuleException("Slug already in use: " + company.getSlug());
        }
        return companyRepository.save(company);
    }

    public Branding saveBranding(UUID companyId, Branding branding) {
        branding.setCompanyId(companyId);
        return brandingRepository.findByCompanyId(companyId)
                .map(existing -> {
                    existing.setLogoUrl(branding.getLogoUrl());
                    existing.setPrimaryColor(branding.getPrimaryColor());
                    existing.setSecondaryColor(branding.getSecondaryColor());
                    existing.setTagline(branding.getTagline());
                    existing.setAboutText(branding.getAboutText());
                    return brandingRepository.save(existing);
                })
                .orElseGet(() -> brandingRepository.save(branding));
    }

    public Settings saveSettings(UUID companyId, Settings settings) {
        settings.setCompanyId(companyId);
        return settingsRepository.findByCompanyId(companyId)
                .map(existing -> {
                    existing.setDefaultCurrency(settings.getDefaultCurrency());
                    existing.setDefaultTaxRate(settings.getDefaultTaxRate());
                    existing.setEstimateValidityDays(settings.getEstimateValidityDays());
                    existing.setEstimateFooterText(settings.getEstimateFooterText());
                    return settingsRepository.save(existing);
                })
                .orElseGet(() -> settingsRepository.save(settings));
    }
}
