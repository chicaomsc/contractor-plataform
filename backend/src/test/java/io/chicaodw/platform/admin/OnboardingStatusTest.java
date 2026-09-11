package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.domain.TeamInvitation;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.infrastructure.persistence.TeamInvitationRepository;
import io.chicaodw.platform.company.api.dto.AddressRequest;
import io.chicaodw.platform.company.api.dto.UpdateBrandingRequest;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.onboarding.api.dto.OnboardingStatusResponse;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import io.chicaodw.platform.servicecatalog.api.dto.ServiceResponse;
import io.chicaodw.platform.servicecatalog.api.dto.UpdateServiceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-018A — {@code GET /onboarding/status}: every flag is derived live from existing
 * entity state, so this class exercises the six completion rules directly against real
 * endpoints/real data (Postgres via {@link AbstractAdminIntegrationTest}), plus
 * independence, tenant isolation and authorization. Pure role-matrix coverage
 * ({@code MANAGER}/{@code MEMBER}/{@code SUPER_ADMIN} → 403) is added to
 * {@code TeamRoleAuthorizationMatrixTest}, alongside every other tenant endpoint's row.
 */
class OnboardingStatusTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired BrandingRepository brandingRepository;
    @Autowired TeamInvitationRepository teamInvitationRepository;

    private UUID companyId(RegisteredOwner owner) {
        return companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
    }

    private OnboardingStatusResponse onboardingStatus(String token) throws Exception {
        String body = mockMvc.perform(get("/onboarding/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, OnboardingStatusResponse.class);
    }

    private void updateCompany(
            String token, String tradeName, String phone, String whatsapp,
            String website, String taxNumber, AddressRequest address) throws Exception {
        UpdateCompanyRequest req =
                new UpdateCompanyRequest(null, tradeName, null, phone, whatsapp, website, taxNumber, null, address);
        mockMvc.perform(put("/company/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private void updateBranding(String token, UpdateBrandingRequest req) throws Exception {
        mockMvc.perform(put("/branding/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private String createService(String token, boolean active) throws Exception {
        String body = mockMvc.perform(post("/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateServiceRequest("Onboarding Service " + System.nanoTime(), null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readValue(body, ServiceResponse.class).id().toString();

        if (!active) {
            mockMvc.perform(put("/services/" + id)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateServiceRequest(null, null, null, null, null, false))))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private String createCustomer(String token, boolean active) throws Exception {
        String body = mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCustomerRequest(
                                "Onboarding Customer " + System.nanoTime(),
                                "onboarding-" + System.nanoTime() + "@example.com",
                                null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readValue(body, CustomerResponse.class).id().toString();

        if (!active) {
            mockMvc.perform(delete("/customers/" + id).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }
        return id;
    }

    private void createDraftEstimate(String token, String customerId) throws Exception {
        mockMvc.perform(post("/estimates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEstimateRequest(
                                UUID.fromString(customerId), "Onboarding Estimate",
                                null, null, null, null, null, null, null, null, null, null))))
                .andExpect(status().isCreated());
    }

    private TeamInvitation createInvitation(UUID companyId, UUID invitedBy) {
        TeamInvitation invitation = new TeamInvitation();
        invitation.setCompanyId(companyId);
        invitation.setEmail("invitee-" + System.nanoTime() + "@example.com");
        invitation.setRole(UserRole.MEMBER);
        invitation.setTokenHash("hash-" + UUID.randomUUID());
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation.setInvitedByUserId(invitedBy);
        return teamInvitationRepository.save(invitation);
    }

    // ── 1. Fresh company ─────────────────────────────────────────────────────

    @Test
    void freshCompany_allFlagsFalse() throws Exception {
        var owner = registerOwner();

        OnboardingStatusResponse response = onboardingStatus(owner.accessToken());

        assertThat(response.companyCompleted()).isFalse();
        assertThat(response.brandingCompleted()).isFalse();
        assertThat(response.servicesCompleted()).isFalse();
        assertThat(response.customerCompleted()).isFalse();
        assertThat(response.estimateCompleted()).isFalse();
        assertThat(response.teamCompleted()).isFalse();
    }

    // ── 2. companyCompleted ───────────────────────────────────────────────────

    @Test
    void companyCompleted_tradeNameAlone_isTrue() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), "Acme Trading", null, null, null, null, null);

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_phoneAlone_isTrue() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, "+351912345678", null, null, null, null);

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_whatsappAlone_isTrue() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, null, "+351912345678", null, null, null);

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_websiteAlone_isTrue() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, null, null, "https://acme.example.com", null, null);

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_taxNumberAlone_isTrue() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, null, null, null, "PT123456789", null);

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_addressStreetAlone_isTrue() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, null, null, null, null,
                new AddressRequest("Rua Principal 1", null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_addressPostalCodeAlone_isTrue() throws Exception {
        // Proves the rule checks every address field, not just street.
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, null, null, null, null,
                new AddressRequest(null, null, "1000-001", null, null));

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isTrue();
    }

    @Test
    void companyCompleted_blankTradeName_isFalse() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), "   ", null, null, null, null, null);

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isFalse();
    }

    @Test
    void companyCompleted_whitespaceOnlyAddress_isFalse() throws Exception {
        var owner = registerOwner();
        updateCompany(owner.accessToken(), null, null, null, null, null,
                new AddressRequest("  ", "\t", null, "", null));

        assertThat(onboardingStatus(owner.accessToken()).companyCompleted()).isFalse();
    }

    // ── 3. brandingCompleted ──────────────────────────────────────────────────

    @Test
    void brandingCompleted_untouchedDefaults_isFalse() throws Exception {
        var owner = registerOwner();

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isFalse();
    }

    @Test
    void brandingCompleted_defaultsResubmittedWithDifferentCasing_isFalse() throws Exception {
        var owner = registerOwner();
        // Same default colors, just lower-case — must still count as "unchanged".
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest("#1e40af", "#3b82f6", "#f59e0b", null, null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isFalse();
    }

    @Test
    void brandingCompleted_primaryColorChanged_isTrue() throws Exception {
        var owner = registerOwner();
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest("#112233", null, null, null, null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isTrue();
    }

    @Test
    void brandingCompleted_secondaryColorChanged_isTrue() throws Exception {
        var owner = registerOwner();
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest(null, "#445566", null, null, null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isTrue();
    }

    @Test
    void brandingCompleted_accentColorChanged_isTrue() throws Exception {
        var owner = registerOwner();
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest(null, null, "#778899", null, null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isTrue();
    }

    @Test
    void brandingCompleted_taglineCustomization_isTrue() throws Exception {
        var owner = registerOwner();
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest(null, null, null, "Quality work, fair prices", null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isTrue();
    }

    @Test
    void brandingCompleted_logoUrlPresent_isTrue() throws Exception {
        // No multipart-upload path in this suite — set directly, same as any other
        // direct-entity-state test fixture already used across this suite (e.g.
        // AbstractAdminIntegrationTest.addMember inserting a User row directly).
        var owner = registerOwner();
        Branding branding = brandingRepository.findByCompanyId(companyId(owner)).orElseThrow();
        branding.setLogoUrl("/uploads/logo.png");
        brandingRepository.save(branding);

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isTrue();
    }

    @Test
    void brandingCompleted_blankTagline_isFalse() throws Exception {
        var owner = registerOwner();
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest(null, null, null, "   ", null, null, null, null));

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isFalse();
    }

    @Test
    void brandingCompleted_nullPrimaryColor_isFalse() throws Exception {
        // Entity column is nullable (no @Column(nullable=false) on Branding.primaryColor)
        // — a null here is simply "no value", not evidence of customization, so it must
        // NOT count as "different from default". No multipart/API path produces this
        // (the request DTO's @Pattern would reject a blank/null submission), so it's set
        // directly, same fixture style as brandingCompleted_logoUrlPresent_isTrue above —
        // this does not weaken any validation, it only exercises the entity-level state
        // that validation permits (the column itself is nullable).
        var owner = registerOwner();
        Branding branding = brandingRepository.findByCompanyId(companyId(owner)).orElseThrow();
        branding.setPrimaryColor(null);
        brandingRepository.save(branding);

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isFalse();
    }

    @Test
    void brandingCompleted_blankPrimaryColor_isFalse() throws Exception {
        // Same reasoning as the null case above — a whitespace-only value is still "no
        // real value", not a customization.
        var owner = registerOwner();
        Branding branding = brandingRepository.findByCompanyId(companyId(owner)).orElseThrow();
        branding.setPrimaryColor("   ");
        brandingRepository.save(branding);

        assertThat(onboardingStatus(owner.accessToken()).brandingCompleted()).isFalse();
    }

    // ── 4. servicesCompleted ──────────────────────────────────────────────────

    @Test
    void servicesCompleted_activeService_isTrue() throws Exception {
        var owner = registerOwner();
        createService(owner.accessToken(), true);

        assertThat(onboardingStatus(owner.accessToken()).servicesCompleted()).isTrue();
    }

    @Test
    void servicesCompleted_inactiveService_isTrue() throws Exception {
        var owner = registerOwner();
        createService(owner.accessToken(), false);

        assertThat(onboardingStatus(owner.accessToken()).servicesCompleted()).isTrue();
    }

    // ── 5. customerCompleted ──────────────────────────────────────────────────

    @Test
    void customerCompleted_activeCustomer_isTrue() throws Exception {
        var owner = registerOwner();
        createCustomer(owner.accessToken(), true);

        assertThat(onboardingStatus(owner.accessToken()).customerCompleted()).isTrue();
    }

    @Test
    void customerCompleted_inactiveCustomer_isTrue() throws Exception {
        var owner = registerOwner();
        createCustomer(owner.accessToken(), false);

        assertThat(onboardingStatus(owner.accessToken()).customerCompleted()).isTrue();
    }

    // ── 6. estimateCompleted ──────────────────────────────────────────────────

    @Test
    void estimateCompleted_draftEstimate_isTrue() throws Exception {
        var owner = registerOwner();
        String customerId = createCustomer(owner.accessToken(), true);
        createDraftEstimate(owner.accessToken(), customerId);

        assertThat(onboardingStatus(owner.accessToken()).estimateCompleted()).isTrue();
    }

    // ── 7. teamCompleted — every invitation status counts ────────────────────

    @Test
    void teamCompleted_pendingInvitation_isTrue() throws Exception {
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();
        createInvitation(companyId(owner), ownerId);

        assertThat(onboardingStatus(owner.accessToken()).teamCompleted()).isTrue();
    }

    @Test
    void teamCompleted_usedInvitation_isTrue() throws Exception {
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();
        TeamInvitation invitation = createInvitation(companyId(owner), ownerId);
        invitation.setUsedAt(Instant.now());
        teamInvitationRepository.save(invitation);

        assertThat(onboardingStatus(owner.accessToken()).teamCompleted()).isTrue();
    }

    @Test
    void teamCompleted_revokedInvitation_isTrue() throws Exception {
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();
        TeamInvitation invitation = createInvitation(companyId(owner), ownerId);
        invitation.setRevokedAt(Instant.now());
        teamInvitationRepository.save(invitation);

        assertThat(onboardingStatus(owner.accessToken()).teamCompleted()).isTrue();
    }

    @Test
    void teamCompleted_expiredInvitation_isTrue() throws Exception {
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();
        TeamInvitation invitation = createInvitation(companyId(owner), ownerId);
        invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        teamInvitationRepository.save(invitation);

        assertThat(onboardingStatus(owner.accessToken()).teamCompleted()).isTrue();
    }

    // ── 8. Independence — one flag flips, the others don't ──────────────────

    @Test
    void independence_onlyServicesCompleted_othersStayFalse() throws Exception {
        var owner = registerOwner();
        createService(owner.accessToken(), true);

        OnboardingStatusResponse response = onboardingStatus(owner.accessToken());

        assertThat(response.servicesCompleted()).isTrue();
        assertThat(response.companyCompleted()).isFalse();
        assertThat(response.brandingCompleted()).isFalse();
        assertThat(response.customerCompleted()).isFalse();
        assertThat(response.estimateCompleted()).isFalse();
        assertThat(response.teamCompleted()).isFalse();
    }

    @Test
    void independence_onlyBrandingCompleted_othersStayFalse() throws Exception {
        var owner = registerOwner();
        updateBranding(owner.accessToken(),
                new UpdateBrandingRequest(null, null, null, "Independence check", null, null, null, null));

        OnboardingStatusResponse response = onboardingStatus(owner.accessToken());

        assertThat(response.brandingCompleted()).isTrue();
        assertThat(response.companyCompleted()).isFalse();
        assertThat(response.servicesCompleted()).isFalse();
        assertThat(response.customerCompleted()).isFalse();
        assertThat(response.estimateCompleted()).isFalse();
        assertThat(response.teamCompleted()).isFalse();
    }

    @Test
    void independence_customerWithoutEstimate_estimateStaysFalse() throws Exception {
        var owner = registerOwner();
        createCustomer(owner.accessToken(), true);

        OnboardingStatusResponse response = onboardingStatus(owner.accessToken());

        assertThat(response.customerCompleted()).isTrue();
        assertThat(response.estimateCompleted()).isFalse();
    }

    // ── 9. Tenant isolation ───────────────────────────────────────────────────

    @Test
    void tenantIsolation_companyBsFullyCompletedDataNeverAffectsCompanyA() throws Exception {
        var ownerB = registerOwner();
        updateCompany(ownerB.accessToken(), "Company B Trading", "+351911111111", null, null, null, null);
        updateBranding(ownerB.accessToken(),
                new UpdateBrandingRequest("#000000", null, null, "Company B tagline", null, null, null, null));
        createService(ownerB.accessToken(), true);
        String customerBId = createCustomer(ownerB.accessToken(), true);
        createDraftEstimate(ownerB.accessToken(), customerBId);
        UUID ownerBId = userRepository.findByEmail(ownerB.email()).orElseThrow().getId();
        createInvitation(companyId(ownerB), ownerBId);
        assertThat(onboardingStatus(ownerB.accessToken()))
                .isEqualTo(new OnboardingStatusResponse(true, true, true, true, true, true));

        var ownerA = registerOwner();

        OnboardingStatusResponse responseA = onboardingStatus(ownerA.accessToken());
        assertThat(responseA).isEqualTo(new OnboardingStatusResponse(false, false, false, false, false, false));
    }

    // ── 10. Authorization (no-token default behavior) ───────────────────────

    @Test
    void noToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/onboarding/status"))
                .andExpect(status().isUnauthorized());
    }
}
