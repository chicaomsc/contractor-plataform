package io.chicaodw.platform.estimate;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.estimate.application.EstimateNumberGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test (needs the DB for the atomic upsert). Each test runs inside a single
 * Spring-managed transaction — required because {@link EstimateNumberGenerator#generate}
 * uses {@code Propagation.MANDATORY} to guarantee it only ever runs as part of the
 * enclosing estimate-creation transaction.
 */
@Transactional
class EstimateNumberGeneratorTest extends AbstractIntegrationTest {

    @Autowired EstimateNumberGenerator numberGenerator;
    @Autowired CompanyRepository       companyRepository;
    @Autowired BrandingRepository      brandingRepository;

    @Test
    void generate_firstCallForCompany_startsAtOne() {
        var companyId = createCompany(null);

        var number = numberGenerator.generate(companyId);

        assertThat(number).isEqualTo("ORC-%d-0001".formatted(Year.now().getValue()));
    }

    @Test
    void generate_sequentialCalls_incrementWithinSameCompanyAndYear() {
        var companyId = createCompany(null);

        var first  = numberGenerator.generate(companyId);
        var second = numberGenerator.generate(companyId);
        var third  = numberGenerator.generate(companyId);

        int year = Year.now().getValue();
        assertThat(first).isEqualTo("ORC-%d-0001".formatted(year));
        assertThat(second).isEqualTo("ORC-%d-0002".formatted(year));
        assertThat(third).isEqualTo("ORC-%d-0003".formatted(year));
    }

    @Test
    void generate_differentCompanies_haveIndependentSequences() {
        var companyA = createCompany(null);
        var companyB = createCompany(null);

        numberGenerator.generate(companyA);
        var firstForB = numberGenerator.generate(companyB);

        assertThat(firstForB).isEqualTo("ORC-%d-0001".formatted(Year.now().getValue()));
    }

    @Test
    void generate_usesBrandingQuotationPrefix_whenConfigured() {
        var companyId = createCompany("PINT");

        var number = numberGenerator.generate(companyId);

        assertThat(number).startsWith("PINT-");
    }

    @Test
    void generate_fallsBackToDefaultPrefix_whenBrandingMissing() {
        var company = new Company();
        company.setName("No Branding Co");
        company.setSlug("no-branding-" + UUID.randomUUID());
        company.setEmail("no-branding@example.com");
        company.setCountry("PT");
        var companyId = companyRepository.save(company).getId();

        var number = numberGenerator.generate(companyId);

        assertThat(number).startsWith("ORC-");
    }

    private UUID createCompany(String quotationPrefix) {
        var company = new Company();
        company.setName("Test Co");
        company.setSlug("test-co-" + UUID.randomUUID());
        company.setEmail("test-co@example.com");
        company.setCountry("PT");
        var companyId = companyRepository.save(company).getId();

        var branding = new Branding();
        branding.setCompanyId(companyId);
        branding.setQuotationPrefix(quotationPrefix);
        brandingRepository.save(branding);

        return companyId;
    }
}
