package io.chicaodw.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for public (unauthenticated) API endpoints.
 * Registers a real user through the full stack, creates data as admin,
 * and verifies the public endpoints return only active items in correct order.
 */
@AutoConfigureMockMvc
class PublicApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;
    private String companySlug;

    @BeforeEach
    void registerAndGetSlug() throws Exception {
        var req = new RegisterRequest(
                "Test Owner",
                "public-api-" + System.nanoTime() + "@example.com",
                "securePass1",
                "Public API Test Co",
                "PT"
        );

        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var authResponse = objectMapper.readValue(body, AuthResponse.class);
        accessToken = authResponse.accessToken();
        companySlug = authResponse.company().slug();
    }

    // ── GET /public/sites/{companySlug} ───────────────────────────────────────

    @Test
    void publicSite_noAuth_returnsPublicCompanyData() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(companySlug))
                .andExpect(jsonPath("$.name").value("Public API Test Co"))
                .andExpect(jsonPath("$.branding").exists());
    }

    @Test
    void publicSite_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}", "nonexistent-company-slug-xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicSite_responseHasNoSensitiveFields() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxNumber").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.companyId").doesNotExist())
                .andExpect(jsonPath("$.branding.companyId").doesNotExist())
                .andExpect(jsonPath("$.branding.quotationPrefix").doesNotExist())
                .andExpect(jsonPath("$.branding.signatureName").doesNotExist());
    }

    @Test
    void publicSite_slugWithAccents_isNormalizedOnOnboarding() throws Exception {
        var auth = registerCompany("accent-" + System.nanoTime() + "@example.com", "Pintáções São João");

        mockMvc.perform(get("/public/sites/{companySlug}", auth.company().slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.startsWith("pintacoes-sao-joao")));
    }

    // ── GET /public/sites/{companySlug}/services ──────────────────────────────

    @Test
    void publicServices_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}/services", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void publicServices_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}/services", "nonexistent-company-slug-xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicServices_returnsOnlyActiveInOrder() throws Exception {
        createService("Serviço Activo A", 1, true);
        createService("Serviço Inactivo",  2, false);
        createService("Serviço Activo B",  0, true);

        mockMvc.perform(get("/public/sites/{companySlug}/services", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Serviço Activo B"))
                .andExpect(jsonPath("$[1].name").value("Serviço Activo A"))
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.name == 'Serviço Inactivo')]").isEmpty());
    }

    @Test
    void publicServices_responseHasNoSensitiveFields() throws Exception {
        createService("Serviço Público", 0, true);

        mockMvc.perform(get("/public/sites/{companySlug}/services", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].slug").exists())
                .andExpect(jsonPath("$[0].companyId").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void publicServices_preservesCompanyIsolation() throws Exception {
        createService("Serviço Empresa A", 0, true);

        var other = registerCompany("other-services-" + System.nanoTime() + "@example.com", "Other Services Co");
        createService(other.accessToken(), "Serviço Empresa B", 0, true);

        mockMvc.perform(get("/public/sites/{companySlug}/services", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Serviço Empresa A')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name == 'Serviço Empresa B')]").isEmpty());
    }

    // ── GET /public/sites/{companySlug}/gallery ───────────────────────────────

    @Test
    void publicGallery_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}/gallery", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void publicGallery_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/public/sites/{companySlug}/gallery", "nonexistent-company-slug-xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicGallery_returnsOnlyActiveItemsFeaturedFirst() throws Exception {
        createGalleryItem("Obra Normal",   0, false, true);
        createGalleryItem("Obra Inactiva", 1, false, false);
        createGalleryItem("Obra Destaque", 2, true,  true);

        mockMvc.perform(get("/public/sites/{companySlug}/gallery", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Obra Destaque"))
                .andExpect(jsonPath("$[?(@.title == 'Obra Inactiva')]").isEmpty());
    }

    @Test
    void publicGallery_responseHasNoSensitiveFields() throws Exception {
        createGalleryItem("Obra Pública", 0, false, true);

        mockMvc.perform(get("/public/sites/{companySlug}/gallery", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].companyId").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void publicGallery_allowsOptionalBeforeAfterImages() throws Exception {
        createGalleryItem("Obra Sem Imagens", 0, false, true);

        mockMvc.perform(get("/public/sites/{companySlug}/gallery", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Obra Sem Imagens"))
                .andExpect(jsonPath("$[0].beforeImageUrl").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].afterImageUrl").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void publicGallery_preservesCompanyIsolation() throws Exception {
        createGalleryItem("Obra Empresa A", 0, false, true);

        var other = registerCompany("other-gallery-" + System.nanoTime() + "@example.com", "Other Gallery Co");
        createGalleryItem(other.accessToken(), "Obra Empresa B", 0, false, true);

        mockMvc.perform(get("/public/sites/{companySlug}/gallery", companySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Obra Empresa A')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.title == 'Obra Empresa B')]").isEmpty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AuthResponse registerCompany(String email, String companyName) throws Exception {
        var req = new RegisterRequest("Test Owner", email, "securePass1", companyName, "PT");

        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(body, AuthResponse.class);
    }

    private void createService(String name, int displayOrder, boolean active) throws Exception {
        createService(accessToken, name, displayOrder, active);
    }

    private void createService(String token, String name, int displayOrder, boolean active) throws Exception {
        var req = new CreateServiceRequest(name, null, null, null, displayOrder, active);
        mockMvc.perform(post("/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private void createGalleryItem(String title, int displayOrder, boolean featured, boolean active) throws Exception {
        createGalleryItem(accessToken, title, displayOrder, featured, active);
    }

    private void createGalleryItem(String token, String title, int displayOrder, boolean featured, boolean active) throws Exception {
        var req = new CreateGalleryRequest(title, null, displayOrder, featured, active);
        mockMvc.perform(post("/gallery")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
