package io.chicaodw.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RegisterIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Register success ──────────────────────────────────────────────────────

    @Test
    void register_returnsCreatedWithTokens() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "João Silva",
                "joao-" + System.nanoTime() + "@example.com",
                "securePass1",
                "Silva & Filhos",
                "PT"
        );

        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(req.email()))
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andExpect(jsonPath("$.company.country").value("PT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse response = objectMapper.readValue(body, AuthResponse.class);
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    // ── Duplicate email ───────────────────────────────────────────────────────

    @Test
    void register_duplicateEmail_returnsConflictWithoutEchoingTheEmail() throws Exception {
        String email = "dup-reg-" + System.nanoTime() + "@example.com";
        RegisterRequest req = new RegisterRequest("Maria", email, "securePass1", "Maria Obras", "PT");

        // first registration must succeed
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // second with same email must fail — 409 (SEC-AUTH-05/Sprint 11B.6D: accepted
        // as a documented risk that duplicate registration is still distinguishable,
        // but the response no longer echoes the submitted email back).
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(email))));
    }

    // ── Bean Validation ───────────────────────────────────────────────────────

    @Test
    void register_shortPassword_returnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("Ana", "ana@example.com", "short", "Ana Co", "PT");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
}
