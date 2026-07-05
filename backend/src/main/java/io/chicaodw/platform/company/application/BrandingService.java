package io.chicaodw.platform.company.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.UpdateBrandingRequest;
import io.chicaodw.platform.company.api.mapper.CompanyMapper;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BrandingService {

    private final BrandingRepository brandingRepository;
    private final CompanyMapper companyMapper;

    @Transactional(readOnly = true)
    public BrandingResponse getBranding(UUID companyId) {
        return brandingRepository.findByCompanyId(companyId)
                .map(companyMapper::toBrandingResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Branding", companyId));
    }

    public BrandingResponse updateBranding(UUID companyId, UpdateBrandingRequest request) {
        var branding = brandingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Branding", companyId));

        if (request.primaryColor()    != null) branding.setPrimaryColor(request.primaryColor());
        if (request.secondaryColor()  != null) branding.setSecondaryColor(request.secondaryColor());
        if (request.accentColor()     != null) branding.setAccentColor(request.accentColor());
        if (request.tagline()         != null) branding.setTagline(request.tagline());
        if (request.aboutText()       != null) branding.setAboutText(request.aboutText());
        if (request.footerText()      != null) branding.setFooterText(request.footerText());
        if (request.quotationPrefix() != null) branding.setQuotationPrefix(request.quotationPrefix());
        if (request.signatureName()   != null) branding.setSignatureName(request.signatureName());

        return companyMapper.toBrandingResponse(brandingRepository.save(branding));
    }
}
