package io.chicaodw.platform.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.CompanyResponse;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyControllerTest extends AbstractControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    // ── GET /company/me ───────────────────────────────────────────────────────

    @Test
    void getProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/company/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_authenticated_returns200() throws Exception {
        var response = companyResponse("Obras Lda");
        when(companyService.getProfile(COMPANY_ID)).thenReturn(response);

        mockMvc.perform(get("/company/me").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Obras Lda"));
    }

    // ── PUT /company/me ───────────────────────────────────────────────────────

    @Test
    void updateProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/company/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_nonOwner_returns403() throws Exception {
        mockMvc.perform(put("/company/me")
                        .with(authentication(nonOwnerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        var body = objectMapper.writeValueAsString(
                new UpdateCompanyRequest(null, null, "not-an-email", null, null, null, null, null, null));

        mockMvc.perform(put("/company/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void updateProfile_validRequest_returns200() throws Exception {
        var request  = new UpdateCompanyRequest("Nova Obras", null, null, null, null, null, null, null, null);
        var response = companyResponse("Nova Obras");

        when(companyService.updateProfile(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(put("/company/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nova Obras"));
    }

    // ── POST /company/logo ────────────────────────────────────────────────────

    @Test
    void uploadLogo_noAuth_returns401() throws Exception {
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart("/company/logo").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadLogo_nonOwner_returns403() throws Exception {
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart("/company/logo").file(file)
                        .with(authentication(nonOwnerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadLogo_validImage_returns200() throws Exception {
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
        var response = brandingResponse("/uploads/company/" + COMPANY_ID + "/logo/logo.png");

        when(companyService.uploadLogo(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(multipart("/company/logo").file(file)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").isNotEmpty());
    }

    // ── DELETE /company/logo ──────────────────────────────────────────────────

    @Test
    void deleteLogo_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/company/logo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteLogo_authenticated_returns204() throws Exception {
        when(companyService.deleteLogo(COMPANY_ID)).thenReturn(brandingResponse(null));

        mockMvc.perform(delete("/company/logo").with(authentication(ownerAuth())))
                .andExpect(status().isNoContent());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static CompanyResponse companyResponse(String name) {
        return new CompanyResponse(COMPANY_ID, name, null, "slug", "info@test.pt",
                null, null, null, null, "PT", null, "ACTIVE");
    }

    private static BrandingResponse brandingResponse(String logoUrl) {
        return new BrandingResponse(UUID.randomUUID(), COMPANY_ID, logoUrl,
                "#1E40AF", null, null, null, null, null, null, null);
    }
}
