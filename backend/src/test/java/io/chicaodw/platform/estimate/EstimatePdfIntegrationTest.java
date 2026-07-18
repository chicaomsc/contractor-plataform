package io.chicaodw.platform.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EstimatePdfIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CustomerService customerService;
    @Autowired EstimateService estimateService;

    private record Session(String accessToken, UUID companyId) {}

    private Session sessionA;
    private Session sessionB;
    private UUID customerIdA;

    @BeforeEach
    void setUp() throws Exception {
        sessionA = register("pdf-a-" + System.nanoTime());
        sessionB = register("pdf-b-" + System.nanoTime());
    }

    @Test
    void downloadPdf_ownEstimate_returns200WithPdfContentAndHeaders() throws Exception {
        UUID estimateId = createEstimate(sessionA, "PDF Test Job");

        var result = mockMvc.perform(get("/estimates/{id}/pdf", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).contains("attachment").contains(".pdf");

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void downloadPdf_noAuth_returns401() throws Exception {
        UUID estimateId = createEstimate(sessionA, "No Auth Job");

        mockMvc.perform(get("/estimates/{id}/pdf", estimateId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadPdf_crossTenantEstimate_returns404() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Tenant A Job");

        mockMvc.perform(get("/estimates/{id}/pdf", estimateId)
                        .header("Authorization", "Bearer " + sessionB.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadPdf_unknownEstimate_returns404() throws Exception {
        mockMvc.perform(get("/estimates/{id}/pdf", UUID.randomUUID())
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadPdf_draftEstimate_isPermitted_andDoesNotChangeStatus() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Draft Job");

        mockMvc.perform(get("/estimates/{id}/pdf", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk());

        var estimate = mockMvc.perform(get("/estimates/{id}", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andReturn().getResponse().getContentAsString();
        assertThat(estimate).contains("\"status\":\"DRAFT\"");
    }

    @Test
    void downloadPdf_containsCustomerSnapshot_unaffectedByLaterCustomerEdits() throws Exception {
        UUID estimateId = createEstimate(sessionA, "Snapshot Job");

        String beforeText = extractText(downloadBytes(estimateId, sessionA));
        assertThat(beforeText).contains("Original Customer Name");

        // Edit the customer's name after the estimate/PDF already exist.
        customerService.updateCustomer(sessionA.companyId(), customerIdA,
                new UpdateCustomerRequest("Renamed Customer", null, null, null, null, null, null));

        String afterText = extractText(downloadBytes(estimateId, sessionA));
        assertThat(afterText).contains("Original Customer Name");
        assertThat(afterText).doesNotContain("Renamed Customer");
    }

    @Test
    void downloadPdf_withoutLogo_stillSucceeds() throws Exception {
        UUID estimateId = createEstimate(sessionA, "No Logo Job");

        mockMvc.perform(get("/estimates/{id}/pdf", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void downloadPdf_withValidLogo_stillSucceeds() throws Exception {
        byte[] png = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
        var logoFile = new MockMultipartFile("file", "logo.png", "image/png", png);
        mockMvc.perform(multipart("/company/logo")
                        .file(logoFile)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk());

        UUID estimateId = createEstimate(sessionA, "Logo Job");

        mockMvc.perform(get("/estimates/{id}/pdf", estimateId)
                        .header("Authorization", "Bearer " + sessionA.accessToken()))
                .andExpect(status().isOk());
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
        customerIdA = customer.id();

        var item = new EstimateItemRequest(null, "Serviço", BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal("100.00"), null);
        var request = new CreateEstimateRequest(customer.id(), title, null, null, null, null, null, null,
                null, null, List.of(item), List.of());
        var estimate = estimateService.createEstimate(session.companyId(), request);
        return estimate.id();
    }

    private byte[] downloadBytes(UUID estimateId, Session session) throws Exception {
        return mockMvc.perform(get("/estimates/{id}/pdf", estimateId)
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (var reader = new PdfReader(pdfBytes)) {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        }
    }
}
