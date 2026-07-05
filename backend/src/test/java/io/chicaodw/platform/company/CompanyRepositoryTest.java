package io.chicaodw.platform.company;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CompanyRepositoryTest extends AbstractIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private BrandingRepository brandingRepository;
    @Autowired private SettingsRepository settingsRepository;

    // ── Company ──────────────────────────────────────────────────────────────

    @Test
    void shouldPersistCompanyAndPopulateAuditFields() {
        Company company = buildCompany("acme-services");

        Company saved = companyRepository.save(company);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Acme Services");
        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void shouldFindCompanyBySlug() {
        companyRepository.save(buildCompany("slug-finder"));

        var found = companyRepository.findBySlug("slug-finder");

        assertThat(found).isPresent();
        assertThat(found.get().getSlug()).isEqualTo("slug-finder");
    }

    @Test
    void shouldReturnEmptyWhenSlugNotFound() {
        var found = companyRepository.findBySlug("nonexistent-slug");
        assertThat(found).isEmpty();
    }

    // ── Branding ─────────────────────────────────────────────────────────────

    @Test
    void shouldPersistBrandingLinkedToCompany() {
        Company company = companyRepository.save(buildCompany("branding-co"));

        Branding branding = new Branding();
        branding.setCompanyId(company.getId());
        branding.setPrimaryColor("#FF5500");
        branding.setTagline("Quality you can see.");

        Branding saved = brandingRepository.save(branding);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCompanyId()).isEqualTo(company.getId());

        var found = brandingRepository.findByCompanyId(company.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTagline()).isEqualTo("Quality you can see.");
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    @Test
    void shouldPersistSettingsWithDefaults() {
        Company company = companyRepository.save(buildCompany("settings-co"));

        Settings settings = new Settings();
        settings.setCompanyId(company.getId());
        settings.setDefaultCurrency("EUR");
        settings.setDefaultTaxRate(new BigDecimal("23.00"));
        settings.setEstimateValidityDays(30);

        Settings saved = settingsRepository.save(settings);

        assertThat(saved.getId()).isNotNull();

        var found = settingsRepository.findByCompanyId(company.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDefaultCurrency()).isEqualTo("EUR");
        assertThat(found.get().getDefaultTaxRate()).isEqualByComparingTo("23.00");
        assertThat(found.get().getEstimateValidityDays()).isEqualTo(30);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Company buildCompany(String slug) {
        Company c = new Company();
        c.setName("Acme Services");
        c.setSlug(slug);
        c.setEmail("contact@example.com");
        c.setCountry("PT");
        c.setStatus(CompanyStatus.ACTIVE);
        return c;
    }
}
