package io.chicaodw.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
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

@AutoConfigureMockMvc
class LoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String email;
    private static final String PASSWORD = "securePass1";

    @BeforeEach
    void registerUser() throws Exception {
        email = "login-test-" + System.nanoTime() + "@example.com";
        RegisterRequest req = new RegisterRequest("Test User", email, PASSWORD, "Test Corp", "PT");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokens() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewAccessToken() throws Exception {
        String registerBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse loginResponse = objectMapper.readValue(registerBody, AuthResponse.class);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(loginResponse.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refresh_invalidToken_returnsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("invalid-token"))))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── /me ───────────────────────────────────────────────────────────────────

    @Test
    void me_withValidToken_returnsFullProfile() throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readValue(body, AuthResponse.class).accessToken();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.company").isNotEmpty())
                .andExpect(jsonPath("$.branding").isNotEmpty())
                .andExpect(jsonPath("$.settings").isNotEmpty());
    }

    @Test
    void me_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
