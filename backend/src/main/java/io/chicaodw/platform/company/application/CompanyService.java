package io.chicaodw.platform.company.application;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.storage.ImageUploadPolicy;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.CompanyResponse;
import io.chicaodw.platform.company.api.dto.PublicSiteBrandingResponse;
import io.chicaodw.platform.company.api.dto.PublicSiteLocationResponse;
import io.chicaodw.platform.company.api.dto.PublicSiteResponse;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import io.chicaodw.platform.company.api.mapper.CompanyMapper;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository  companyRepository;
    private final BrandingRepository brandingRepository;
    private final SettingsRepository settingsRepository;
    private final StorageService     storageService;
    private final ImageUploadPolicy  imageUploadPolicy;
    private final CompanyMapper      companyMapper;

    // ── Internal (used by AuthService during onboarding) ─────────────────────

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

    @Transactional(readOnly = true)
    public PublicSiteResponse getPublicSite(String slug) {
        var company = findBySlug(slug);
        var branding = brandingRepository.findByCompanyId(company.getId()).orElse(null);
        var address = company.getAddress();

        return new PublicSiteResponse(
                company.getSlug(),
                company.getName(),
                company.getTradeName(),
                company.getPhone(),
                company.getWhatsapp(),
                company.getWebsite(),
                address != null
                        ? new PublicSiteLocationResponse(address.getCity(), address.getRegion(), address.getCountry())
                        : null,
                branding != null
                        ? new PublicSiteBrandingResponse(
                                branding.getLogoUrl(),
                                branding.getPrimaryColor(),
                                branding.getSecondaryColor(),
                                branding.getAccentColor(),
                                branding.getTagline(),
                                branding.getAboutText(),
                                branding.getFooterText()
                        )
                        : null
        );
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
                    existing.setAccentColor(branding.getAccentColor());
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
                    existing.setLocale(settings.getLocale());
                    existing.setTimezone(settings.getTimezone());
                    return settingsRepository.save(existing);
                })
                .orElseGet(() -> settingsRepository.save(settings));
    }

    // ── Profile management (Sprint 5) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public CompanyResponse getProfile(UUID companyId) {
        return companyMapper.toCompanyResponse(findById(companyId));
    }

    public CompanyResponse updateProfile(UUID companyId, UpdateCompanyRequest request) {
        var company = findById(companyId);

        if (request.name()      != null) company.setName(request.name());
        if (request.tradeName() != null) company.setTradeName(request.tradeName());
        if (request.email()     != null) company.setEmail(request.email());
        if (request.phone()     != null) company.setPhone(request.phone());
        if (request.whatsapp()  != null) company.setWhatsapp(request.whatsapp());
        if (request.website()   != null) company.setWebsite(request.website());
        if (request.taxNumber() != null) company.setTaxNumber(request.taxNumber());
        if (request.country()   != null) company.setCountry(request.country());

        if (request.address() != null) {
            var addr    = request.address();
            var current = company.getAddress() != null ? company.getAddress() : Address.builder().build();
            company.setAddress(Address.builder()
                    .street(addr.street()     != null ? addr.street()     : current.getStreet())
                    .city(addr.city()         != null ? addr.city()       : current.getCity())
                    .postalCode(addr.postalCode() != null ? addr.postalCode() : current.getPostalCode())
                    .region(addr.region()     != null ? addr.region()     : current.getRegion())
                    .country(addr.country()   != null ? addr.country()    : current.getCountry())
                    .build());
        }

        return companyMapper.toCompanyResponse(companyRepository.save(company));
    }

    // ── Logo management ───────────────────────────────────────────────────────

    public BrandingResponse uploadLogo(UUID companyId, MultipartFile file) {
        imageUploadPolicy.validate(file);

        var branding = brandingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Branding", companyId));

        // Delete the existing logo file before storing the new one
        if (branding.getLogoUrl() != null) {
            storageService.delete(branding.getLogoUrl());
        }

        String folder  = "company/" + companyId + "/logo";
        String logoUrl = storageService.store(folder, file);
        branding.setLogoUrl(logoUrl);

        return companyMapper.toBrandingResponse(brandingRepository.save(branding));
    }

    public BrandingResponse deleteLogo(UUID companyId) {
        var branding = brandingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Branding", companyId));

        if (branding.getLogoUrl() != null) {
            storageService.delete(branding.getLogoUrl());
            branding.setLogoUrl(null);
            brandingRepository.save(branding);
        }

        return companyMapper.toBrandingResponse(branding);
    }
}
