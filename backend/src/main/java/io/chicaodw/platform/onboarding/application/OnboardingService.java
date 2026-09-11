package io.chicaodw.platform.onboarding.application;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.customer.infrastructure.persistence.CustomerRepository;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.TeamInvitationRepository;
import io.chicaodw.platform.onboarding.api.dto.OnboardingStatusResponse;
import io.chicaodw.platform.servicecatalog.infrastructure.persistence.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * DT-018A — computes the onboarding checklist status for a Company entirely from
 * existing entity state; nothing is ever written here (read-only), and there is no
 * "mark step complete" mutation anywhere in this module — every flag is a live fact
 * about real data, never a client-settable claim (see DT-018 read-only audit §12/§18
 * for the persisted-vs-derived rationale).
 *
 * <p>Reuses the existing tenant repositories exactly as-is — no new domain logic is
 * duplicated, only the six completion rules themselves, which have no other home.
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    // Registration-time branding defaults, hardcoded identically by both
    // AuthService.register and AdminCompanyService.createCompanyWithOwner — every
    // Company starts with exactly these three colors, so "color is non-null" can never
    // distinguish a customized Company from a brand-new one (DT-018 audit §4). Only a
    // value that differs from these counts as evidence of customization.
    private static final String DEFAULT_PRIMARY_COLOR = "#1E40AF";
    private static final String DEFAULT_SECONDARY_COLOR = "#3B82F6";
    private static final String DEFAULT_ACCENT_COLOR = "#F59E0B";

    private final CompanyRepository companyRepository;
    private final BrandingRepository brandingRepository;
    private final ServiceRepository serviceRepository;
    private final CustomerRepository customerRepository;
    private final EstimateRepository estimateRepository;
    private final TeamInvitationRepository teamInvitationRepository;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(UUID companyId) {
        return new OnboardingStatusResponse(
                isCompanyCompleted(companyId),
                isBrandingCompleted(companyId),
                serviceRepository.existsByCompanyId(companyId),
                customerRepository.existsByCompanyId(companyId),
                estimateRepository.existsByCompanyId(companyId),
                teamInvitationRepository.existsByCompanyId(companyId));
    }

    /** True once at least one optional profile field has been filled in beyond what
     * registration already captures (name/slug/email/country) — blank/whitespace-only
     * strings never count (DT-018A decision #1). */
    private boolean isCompanyCompleted(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        return hasText(company.getTradeName())
                || hasText(company.getPhone())
                || hasText(company.getWhatsapp())
                || hasText(company.getWebsite())
                || hasText(company.getTaxNumber())
                || hasAddressData(company.getAddress());
    }

    private boolean hasAddressData(Address address) {
        if (address == null) {
            return false;
        }
        return hasText(address.getStreet())
                || hasText(address.getCity())
                || hasText(address.getPostalCode())
                || hasText(address.getRegion())
                || hasText(address.getCountry());
    }

    /** True once there is real evidence of customization — never "a color is set", since
     * every Company already has all three colors from creation (DT-018A decision #2).
     * Branding is always created alongside its Company (AuthService/AdminCompanyService),
     * so a missing row here signals a data-integrity bug, not "not yet configured" —
     * same reasoning as {@code BrandingService.getBranding}. */
    private boolean isBrandingCompleted(UUID companyId) {
        Branding branding = brandingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Branding", companyId));
        return hasText(branding.getLogoUrl())
                || hasText(branding.getTagline())
                || hasText(branding.getAboutText())
                || hasText(branding.getFooterText())
                || hasText(branding.getQuotationPrefix())
                || hasText(branding.getSignatureName())
                || differsFromDefault(branding.getPrimaryColor(), DEFAULT_PRIMARY_COLOR)
                || differsFromDefault(branding.getSecondaryColor(), DEFAULT_SECONDARY_COLOR)
                || differsFromDefault(branding.getAccentColor(), DEFAULT_ACCENT_COLOR);
    }

    /** Case-insensitive HEX comparison (DT-018A decision #2, corrected). Conservative by
     * design: null/blank is never treated as "customized" — it is simply the absence of
     * a value, not evidence the OWNER changed anything — so it never counts on its own
     * here. Only a genuinely different, non-blank color counts. */
    private boolean differsFromDefault(String actual, String defaultValue) {
        return hasText(actual) && !actual.equalsIgnoreCase(defaultValue);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
