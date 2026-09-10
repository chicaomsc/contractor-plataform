package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-017A — the authorization matrix from
 * docs/design/DT-017A-roles-authorization-foundation.md, exercised end-to-end against
 * real endpoints/real roles/real companies, not just re-asserted as a table. One
 * parameterized test per business area (row of the matrix); every {@code @PreAuthorize}
 * touched by DT-017A is covered by exactly one of these. SUPER_ADMIN is included in
 * every case specifically to prove §7/§8 of the phase spec: generalizing OWNER's
 * {@code @PreAuthorize} to the tenant roles must never accidentally admit the platform
 * role too.
 */
class TeamRoleAuthorizationMatrixTest extends AbstractAdminIntegrationTest {

    private record TenantSession(String token, String companySlug) {}

    /** Registers a fresh Company + OWNER, then — unless {@code role} is OWNER itself —
     * flips that user's role directly in the database and re-logs-in so the returned
     * token's {@code role} claim reflects the target role (the claim is baked in at
     * login time, so a re-login after the DB change is required). */
    private TenantSession tenantSession(UserRole role) throws Exception {
        RegisteredOwner owner = registerOwner();
        if (role != UserRole.OWNER) {
            User user = userRepository.findByEmail(owner.email()).orElseThrow();
            user.setRole(role);
            userRepository.save(user);
        }
        String token = login(owner.email(), owner.password());
        return new TenantSession(token, owner.companySlug());
    }

    private String tokenFor(String roleName) throws Exception {
        if ("SUPER_ADMIN".equals(roleName)) {
            return createSuperAdminAndLogin();
        }
        return tenantSession(UserRole.valueOf(roleName)).token();
    }

    // ── Clientes ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /customers -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,200", "SUPER_ADMIN,403"})
    void customers_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/customers").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    // ── Orçamentos ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /estimates -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,200", "SUPER_ADMIN,403"})
    void estimates_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/estimates").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest(name = "{0} on GET /estimates/id/share -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,200", "SUPER_ADMIN,403"})
    void estimateShare_matrix(String roleName, int expectedStatus) throws Exception {
        // A nonexistent estimate id is enough here — the matrix is about whether the
        // role clears @PreAuthorize at all, not about the resource existing. A 404
        // from the service layer (wrong id) would be indistinguishable in intent from
        // a 200 for this purpose, so instead every non-permitted role must be stopped
        // at 403 before ever reaching that lookup — a permitted role reaching the
        // lookup (and 404ing on a made-up id) still proves it cleared authorization.
        String token = tokenFor(roleName);
        var result = mockMvc.perform(get("/estimates/" + java.util.UUID.randomUUID() + "/share")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
        if (expectedStatus == 403) {
            org.assertj.core.api.Assertions.assertThat(result).isEqualTo(403);
        } else {
            // Permitted role: authorization passed, so the only possible outcome for a
            // made-up id is "not found" — never 403.
            org.assertj.core.api.Assertions.assertThat(result).isEqualTo(404);
        }
    }

    // ── Serviços ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /services -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,403", "SUPER_ADMIN,403"})
    void services_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/services").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    // ── Galeria ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /gallery -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,403", "SUPER_ADMIN,403"})
    void gallery_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/gallery").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    // ── Branding ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /branding/me -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,403", "SUPER_ADMIN,403"})
    void branding_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/branding/me").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    // ── Empresa — leitura ────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /company/me -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,200", "MEMBER,200", "SUPER_ADMIN,403"})
    void companyRead_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    // ── Empresa — alteração ──────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on PUT /company/me -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,403", "MEMBER,403", "SUPER_ADMIN,403"})
    void companyWrite_matrix(String roleName, int expectedStatus) throws Exception {
        // Every field is optional on UpdateCompanyRequest — an all-null body is valid,
        // so @Valid never rejects it before @PreAuthorize gets a chance to run; the
        // response status observed here is authorization's alone, not validation's.
        String body = objectMapper.writeValueAsString(
                new UpdateCompanyRequest(null, null, null, null, null, null, null, null, null));

        mockMvc.perform(put("/company/me")
                        .header("Authorization", "Bearer " + tokenFor(roleName))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} on GET /settings/me -> {1}")
    @CsvSource({"OWNER,200", "MANAGER,403", "MEMBER,403", "SUPER_ADMIN,403"})
    void settings_matrix(String roleName, int expectedStatus) throws Exception {
        mockMvc.perform(get("/settings/me").header("Authorization", "Bearer " + tokenFor(roleName)))
                .andExpect(status().is(expectedStatus));
    }

    // ── Cross-tenant (§16) ───────────────────────────────────────────────────

    /**
     * MANAGER/MEMBER of Company A must never reach a resource that belongs to Company
     * B — same anti-IDOR contract already enforced for OWNER (company-scoped lookup,
     * 404 rather than 403, so existence of the resource is never leaked either).
     */
    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"MANAGER", "MEMBER"})
    void crossTenant_customerOfAnotherCompany_isNotFound(UserRole role) throws Exception {
        RegisteredOwner ownerB = registerOwner();
        String customerIdInCompanyB = createCustomer(ownerB.accessToken());

        TenantSession sessionA = tenantSession(role);

        mockMvc.perform(get("/customers/" + customerIdInCompanyB)
                        .header("Authorization", "Bearer " + sessionA.token()))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"MANAGER", "MEMBER"})
    void crossTenant_estimateOfAnotherCompany_isNotFound(UserRole role) throws Exception {
        RegisteredOwner ownerB = registerOwner();
        String estimateIdInCompanyB = createEstimate(ownerB.accessToken());

        TenantSession sessionA = tenantSession(role);

        mockMvc.perform(get("/estimates/" + estimateIdInCompanyB)
                        .header("Authorization", "Bearer " + sessionA.token()))
                .andExpect(status().isNotFound());
    }

    private String createCustomer(String ownerToken) throws Exception {
        String body = mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCustomerRequest(
                                "Cross-tenant Customer",
                                "cross-tenant-" + System.nanoTime() + "@example.com",
                                null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, CustomerResponse.class).id().toString();
    }

    private String createEstimate(String ownerToken) throws Exception {
        String customerId = createCustomer(ownerToken);
        String body = mockMvc.perform(post("/estimates")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEstimateRequest(
                                java.util.UUID.fromString(customerId), "Cross-tenant Estimate",
                                null, null, null, null, null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, EstimateResponse.class).id().toString();
    }
}
