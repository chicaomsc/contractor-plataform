package io.chicaodw.platform.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the public-surface findings fixed in Sprint 11B.6B: a deactivated company's
 * services, gallery, estimate shares, and uploaded files must all become unavailable
 * (404) exactly like they would for a company that never existed — never a distinct
 * signal (SEC-TENANT-01/02/03, DT-011B.5 §9 HARD-05).
 */
class PublicSurfaceCompanyInactiveTest extends AbstractAdminIntegrationTest {

    @Autowired io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository companyRepository;

    @Test
    void services_activeThenInactiveThenReactivated() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        createService(owner.accessToken(), "Pintura Exterior");

        mockMvc.perform(get("/public/sites/{slug}/services", owner.companySlug()))
                .andExpect(status().isOk());

        setStatus(adminToken, companyId, "INACTIVE");
        mockMvc.perform(get("/public/sites/{slug}/services", owner.companySlug()))
                .andExpect(status().isNotFound());

        setStatus(adminToken, companyId, "ACTIVE");
        mockMvc.perform(get("/public/sites/{slug}/services", owner.companySlug()))
                .andExpect(status().isOk());
    }

    @Test
    void gallery_activeThenInactiveThenReactivated() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        createGalleryItem(owner.accessToken(), "Obra Concluída");

        mockMvc.perform(get("/public/sites/{slug}/gallery", owner.companySlug()))
                .andExpect(status().isOk());

        setStatus(adminToken, companyId, "INACTIVE");
        mockMvc.perform(get("/public/sites/{slug}/gallery", owner.companySlug()))
                .andExpect(status().isNotFound());

        setStatus(adminToken, companyId, "ACTIVE");
        mockMvc.perform(get("/public/sites/{slug}/gallery", owner.companySlug()))
                .andExpect(status().isOk());
    }

    @Test
    void estimateShare_viewAndPdf_activeThenInactiveThenReactivated() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        String customerId = createCustomer(owner.accessToken());
        UUID estimateId = createEstimate(owner.accessToken(), customerId);
        String token = createShare(owner.accessToken(), estimateId);

        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isOk());
        mockMvc.perform(get("/public/share/{token}/pdf", token)).andExpect(status().isOk());

        setStatus(adminToken, companyId, "INACTIVE");
        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isNotFound());
        mockMvc.perform(get("/public/share/{token}/pdf", token)).andExpect(status().isNotFound());

        setStatus(adminToken, companyId, "ACTIVE");
        mockMvc.perform(get("/public/share/{token}", token)).andExpect(status().isOk());
        mockMvc.perform(get("/public/share/{token}/pdf", token)).andExpect(status().isOk());
    }

    @Test
    void uploadedLogo_activeThenInactiveThenReactivated() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        var file = new MockMultipartFile("file", "logo.png", "image/png", pngBytes());
        String logoBody = mockMvc.perform(multipart("/company/logo").file(file)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String logoUrl = objectMapper.readTree(logoBody).get("logoUrl").asText();
        assertThat(logoUrl).startsWith("/uploads/company/" + companyId + "/logo/");

        mockMvc.perform(get(logoUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        setStatus(adminToken, companyId, "INACTIVE");
        mockMvc.perform(get(logoUrl)).andExpect(status().isNotFound());

        setStatus(adminToken, companyId, "ACTIVE");
        mockMvc.perform(get(logoUrl)).andExpect(status().isOk());
    }

    @Test
    void uploads_pathTraversal_stillBlocked() throws Exception {
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        // Spring Security's StrictHttpFirewall rejects ".." path segments outright (400)
        // before the request ever reaches PublicUploadController/StorageService — an
        // even stronger guarantee than the storage-layer normalize()+startsWith() check.
        mockMvc.perform(get("/uploads/company/" + companyId + "/logo/../../../etc/passwd"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploads_unknownCompanyId_returns404() throws Exception {
        mockMvc.perform(get("/uploads/company/" + UUID.randomUUID() + "/logo/whatever.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveCompany_andUnknownSlug_returnIndistinguishable404() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        setStatus(adminToken, companyId, "INACTIVE");

        String inactiveBody = mockMvc.perform(get("/public/sites/{slug}/services", owner.companySlug()))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String unknownBody = mockMvc.perform(get("/public/sites/{slug}/services", "no-such-company-xyz"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        JsonNode inactiveJson = objectMapper.readTree(inactiveBody);
        JsonNode unknownJson = objectMapper.readTree(unknownBody);
        assertThat(inactiveJson.get("status")).isEqualTo(unknownJson.get("status"));
        assertThat(inactiveJson.has("title")).isEqualTo(unknownJson.has("title"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void createService(String token, String name) throws Exception {
        var req = new CreateServiceRequest(name, null, null, null, 0, true);
        mockMvc.perform(post("/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private void createGalleryItem(String token, String title) throws Exception {
        var req = new CreateGalleryRequest(title, null, 0, false, true);
        mockMvc.perform(post("/gallery")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String createCustomer(String token) throws Exception {
        var req = new CreateCustomerRequest("Cliente Teste", "cliente@example.com", "912345678", null, null, null);
        String body = mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private UUID createEstimate(String token, String customerId) throws Exception {
        var item = new EstimateItemRequest(null, "Serviço", BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal("100.00"), null);
        var req = new CreateEstimateRequest(UUID.fromString(customerId), "Orçamento Teste", null, null, null, null, null, null,
                null, null, List.of(item), List.of());
        String body = mockMvc.perform(post("/estimates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private String createShare(String token, UUID estimateId) throws Exception {
        String body = mockMvc.perform(post("/estimates/{id}/share", estimateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private static byte[] pngBytes() throws java.io.IOException {
        var image = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
