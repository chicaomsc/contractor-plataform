package io.chicaodw.platform.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.company.api.dto.SettingsResponse;
import io.chicaodw.platform.company.api.dto.UpdateSettingsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsControllerTest extends AbstractControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── GET /settings/me ──────────────────────────────────────────────────────

    @Test
    void getSettings_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/settings/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSettings_authenticated_returns200() throws Exception {
        var response = settingsResponse("EUR");
        when(settingsService.getSettings(COMPANY_ID)).thenReturn(response);

        mockMvc.perform(get("/settings/me").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("EUR"));
    }

    // ── PUT /settings/me ──────────────────────────────────────────────────────

    @Test
    void updateSettings_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/settings/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSettings_nonOwner_returns403() throws Exception {
        mockMvc.perform(put("/settings/me")
                        .with(authentication(nonOwnerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSettings_invalidCurrencyLength_returns400() throws Exception {
        var body = objectMapper.writeValueAsString(
                new UpdateSettingsRequest("EU", null, null, null, null, null, null, null, null));

        mockMvc.perform(put("/settings/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void updateSettings_partialUpdate_returns200() throws Exception {
        var request  = new UpdateSettingsRequest(null, new BigDecimal("6.00"), null, null, null, null, null, null, null);
        var response = settingsResponse("EUR");

        when(settingsService.updateSettings(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(put("/settings/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("EUR"));
    }

    @Test
    void updateSettings_fullUpdate_returns200() throws Exception {
        var request  = new UpdateSettingsRequest("USD", new BigDecimal("20.00"),
                60, "Rodapé", "en-US", "America/New_York", "MM/dd/yyyy", "en-US", new BigDecimal("40.00"));
        var response = settingsResponse("USD");

        when(settingsService.updateSettings(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(put("/settings/me")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("USD"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static SettingsResponse settingsResponse(String currency) {
        return new SettingsResponse(UUID.randomUUID(), COMPANY_ID, currency,
                BigDecimal.ZERO, 30, null, "pt-PT", "Europe/Lisbon", "dd/MM/yyyy", "pt-PT", new BigDecimal("50.00"));
    }
}
