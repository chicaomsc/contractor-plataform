package io.chicaodw.platform.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.UpdateBrandingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrandingControllerTest extends AbstractControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── GET /branding/me ──────────────────────────────────────────────────────

    @Test
    void getBranding_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/branding/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBranding_authenticated_returns200() throws Exception {
        var response = brandingResponse("#1E40AF");
        when(brandingService.getBranding(COMPANY_ID)).thenReturn(response);

        mockMvc.perform(get("/branding/me").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#1E40AF"));
    }

    // ── PUT /branding/me ──────────────────────────────────────────────────────

    @Test
    void updateBranding_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/branding/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateBranding_nonOwner_returns403() throws Exception {
        mockMvc.perform(put("/branding/me")
                        .with(authentication(nonOwnerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBranding_invalidHexColor_returns400() throws Exception {
        var body = objectMapper.writeValueAsString(
                new UpdateBrandingRequest("not-a-color", null, null, null, null, null, null, null));

        mockMvc.perform(put("/branding/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void updateBranding_partialUpdate_returns200() throws Exception {
        var request  = new UpdateBrandingRequest(null, null, null, "Tagline nova", null, null, null, null);
        var response = brandingResponse("#1E40AF");

        when(brandingService.updateBranding(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(put("/branding/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#1E40AF"));
    }

    @Test
    void updateBranding_fullUpdate_returns200() throws Exception {
        var request  = new UpdateBrandingRequest("#FF0000", "#00FF00", "#0000FF",
                "Tagline", "Sobre nós", "Rodapé", "ORC", "João");
        var response = brandingResponse("#FF0000");

        when(brandingService.updateBranding(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(put("/branding/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#FF0000"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static BrandingResponse brandingResponse(String primary) {
        return new BrandingResponse(UUID.randomUUID(), COMPANY_ID, null,
                primary, null, null, null, null, null, null, null);
    }
}
