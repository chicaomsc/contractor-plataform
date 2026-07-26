package io.chicaodw.platform.company;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runtime Host -> tenant slug resolution (DT-011A.7 §16/§17, corrected). GET
 * /public/tenant takes no query parameter — it resolves the effective {@code Host}
 * header of the actual request, never a client-supplied value and never
 * X-Forwarded-Host (see PublicTenantController/TenantResolutionService).
 *
 * Uses its own DynamicPropertySource overrides, so Spring Test spins up a separate
 * cached context from the shared one used by other integration tests (unavoidable
 * given the property values differ) — see TenantResolutionNoDefaultIntegrationTest for
 * the "no fallback configured" case, which needs yet another distinct property set.
 */
@AutoConfigureMockMvc
class TenantResolutionIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CompanyRepository companyRepository;

    @DynamicPropertySource
    static void tenantProperties(DynamicPropertyRegistry registry) {
        registry.add("app.platform.base-domain", () -> "localhost");
        registry.add("app.platform.default-tenant-slug", () -> "tenant-default-fallback");
    }

    @Test
    void hostForTenantA_resolvesToTenantA() throws Exception {
        String slugA = createCompany("tenant-a-" + System.nanoTime(), CompanyStatus.ACTIVE);

        mockMvc.perform(get("/public/tenant").header("Host", slugA + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slugA));
    }

    @Test
    void hostForTenantB_resolvesToTenantB_independentlyOfTenantA() throws Exception {
        String slugA = createCompany("tenant-a-" + System.nanoTime(), CompanyStatus.ACTIVE);
        String slugB = createCompany("tenant-b-" + System.nanoTime(), CompanyStatus.ACTIVE);

        mockMvc.perform(get("/public/tenant").header("Host", slugA + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slugA));

        mockMvc.perform(get("/public/tenant").header("Host", slugB + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slugB));
    }

    @Test
    void hostWithExplicitPort_isStrippedBeforeResolution() throws Exception {
        String slug = createCompany("tenant-port-" + System.nanoTime(), CompanyStatus.ACTIVE);

        mockMvc.perform(get("/public/tenant").header("Host", slug + ".localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    void bareLocalhostHost_fallsBackToDefaultTenantSlug() throws Exception {
        createCompany("tenant-default-fallback", CompanyStatus.ACTIVE);

        // "localhost" itself has no subdomain label — it never matches "{slug}.localhost".
        mockMvc.perform(get("/public/tenant").header("Host", "localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("tenant-default-fallback"));
    }

    @Test
    void unknownHost_fallsBackToDefaultTenantSlug() throws Exception {
        createCompany("tenant-default-fallback", CompanyStatus.ACTIVE);

        mockMvc.perform(get("/public/tenant").header("Host", "totally-unknown-host.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("tenant-default-fallback"));
    }

    @Test
    void xForwardedHostHeader_isIgnored_onlyTheRealHostControlsResolution() throws Exception {
        String slugA = createCompany("tenant-a-" + System.nanoTime(), CompanyStatus.ACTIVE);
        String slugB = createCompany("tenant-b-" + System.nanoTime(), CompanyStatus.ACTIVE);

        mockMvc.perform(get("/public/tenant")
                        .header("Host", slugB + ".localhost")
                        .header("X-Forwarded-Host", slugA + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slugB));
    }

    @Test
    void inactiveCompany_hostStillResolvesToSlug_butPublicSiteIsNotFound() throws Exception {
        String slug = createCompany("tenant-inactive-" + System.nanoTime(), CompanyStatus.INACTIVE);

        mockMvc.perform(get("/public/tenant").header("Host", slug + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));

        mockMvc.perform(get("/public/sites/" + slug))
                .andExpect(status().isNotFound());
    }

    /**
     * Idempotent by design: "tenant-default-fallback" is a fixed slug (it must match
     * the DynamicPropertySource value above) reused by two different test methods in
     * this class, and JUnit 5 does not guarantee method execution order — whichever
     * runs second must not try to insert a second row with the same unique slug.
     */
    private String createCompany(String slug, CompanyStatus status) {
        Company company = companyRepository.findBySlug(slug).orElseGet(() -> {
            Company created = new Company();
            created.setName("Tenant " + slug);
            created.setSlug(slug);
            created.setEmail(slug + "@example.com");
            created.setCountry("PT");
            return created;
        });
        company.setStatus(status);
        companyRepository.save(company);
        return slug;
    }
}
