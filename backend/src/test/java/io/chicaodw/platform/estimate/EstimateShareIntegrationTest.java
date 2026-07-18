package io.chicaodw.platform.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EstimateShareIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CustomerService customerService;
    @Autowired EstimateService estimateService;
    @Autowired EstimateShareRepository estimateShareRepository;

    private record Session(String accessToken, UUID companyId) {}

    private Session sessionA;
    private Session sessionB;

    @BeforeEach
    void setUp() throws Exception {
        sessionA = register("share-a-" + System.nanoTime());
        sessionB = register("share-b-" + System.nanoTime());
    }

    @Test
    void createShare_ownEstimate_returns201WithUsableTokenAndDefaultExpiry() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Share Test Job");

        var response = createShare(estimateId, sessionA, "{}");

        assertThat(response.get("token").asText()).isNotBlank();
        assertThat(response.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(response.has("companyId")).isFalse();
    }

    @Test
    void createShare_noAuth_returns401() throws Exception {
        UUID estimateId = createEstimate(sessionA, "No Auth Job");

        mockMvc.perform(post("/estimates/{id}/share", estimateId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createShare_crossTenantEstimate_returns404() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Tenant A Job");

        mockMvc.perform(post("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionB.accessToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createShare_calledTwice_revokesTheFirstToken() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Regenerate Job");

        var first = createShare(estimateId, sessionA, "{}");
        String firstToken = first.get("token").asText();
        var second = createShare(estimateId, sessionA, "{}");
        String secondToken = second.get("token").asText();

        assertThat(firstToken).isNotEqualTo(secondToken);
        mockMvc.perform(get("/public/share/{token}", firstToken)).andExpect(status().isNotFound());
        mockMvc.perform(get("/public/share/{token}", secondToken)).andExpect(status().isOk());
    }

    @Test
    void getShare_afterCreate_returnsStatusWithoutToken() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Status Job");
        createShare(estimateId, sessionA, "{}");

        mockMvc.perform(get("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void getShare_noShareCreated_returns404() throws Exception {
        UUID estimateId = createEstimate(sessionA, "No Share Job");

        mockMvc.perform(get("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokeShare_activeShare_invalidatesThePublicLink() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Revoke Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        mockMvc.perform(delete("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isNotFound());
        mockMvc.perform(get("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void revokeShare_crossTenant_returns404_andDoesNotRevoke() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Cross Revoke Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        mockMvc.perform(delete("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionB.accessToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isOk());
    }

    @Test
    void publicView_expiredShare_returns404() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Expired Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();
        expireTheOnlyShareFor(estimateId);

        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isNotFound());
    }

    @Test
    void publicView_unknownToken_returns404() throws Exception {
        mockMvc.perform(get("/public/share/{token}", "totally-unknown-token")).andExpect(status().isNotFound());
    }

    @Test
    void publicView_validShare_exposesOnlyCustomerFacingFields_neverInternalIds() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Sanitized Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        String body = mockMvc.perform(get("/public/share/{token}", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(sessionA.companyId().toString());
        assertThat(body).doesNotContain(estimateId.toString());
        assertThat(body).contains("Original Customer Name");
        assertThat(body).contains("Sanitized Job");
    }

    @Test
    void publicView_incrementsAccessCountAndSetsLastAccessAt() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Audit Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isOk());
        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isOk());

        mockMvc.perform(get("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessCount").value(2))
                .andExpect(jsonPath("$.lastAccessAt").exists());
    }

    @Test
    void publicPdf_validShare_returnsTheSamePdfPipelineOutput() throws Exception {
        UUID estimateId = createEstimate(sessionA, "PDF Share Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        var result = mockMvc.perform(get("/public/share/{token}/pdf", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void publicPdf_revokedShare_returns404() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Revoked PDF Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        mockMvc.perform(delete("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/public/share/{token}/pdf", token)).andExpect(status().isNotFound());
    }

    @Test
    void deletingTheEstimate_invalidatesItsShare() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Deleted Estimate Job");
        String token = createShare(estimateId, sessionA, "{}").get("token").asText();

        mockMvc.perform(delete("/estimates/{id}", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Session register(String suffix) throws Exception {
        var request = new RegisterRequest("Owner " + suffix, suffix + "@example.com", "securePass1", "Company " + suffix, "PT");
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var auth = objectMapper.readValue(body, AuthResponse.class);
        return new Session(auth.accessToken(), auth.company().id());
    }

    private UUID createEstimate(Session session, String title) {
        var customer = customerService.createCustomer(session.companyId(), new CreateCustomerRequest(
                "Original Customer Name", "customer@example.com", "912345678", null, null, null));

        var item = new EstimateItemRequest(null, "Serviço", BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal("100.00"), null);
        var request = new CreateEstimateRequest(customer.id(), title, null, null, null, null, null, null,
                null, null, List.of(item), List.of());
        var estimate = estimateService.createEstimate(session.companyId(), request);
        return estimate.id();
    }

    private com.fasterxml.jackson.databind.JsonNode createShare(UUID estimateId, Session session, String body) throws Exception {
        String response = mockMvc.perform(post("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private void expireTheOnlyShareFor(UUID estimateId) {
        var share = estimateShareRepository.findAll().stream()
                .filter(s -> s.getEstimateId().equals(estimateId))
                .findFirst().orElseThrow();
        share.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        estimateShareRepository.save(share);
    }
}
