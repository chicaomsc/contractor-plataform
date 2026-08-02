package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /auth/logout — DT-011B.5 §9 HARD-02 (SEC-AUTH-13). Revokes the refresh token
 * used to call it; deliberately leaves the caller's access token untouched (it stays
 * valid until its own natural expiry — this is the documented, intentional scope of
 * this endpoint, not a gap).
 */
class LogoutTest extends AbstractAdminIntegrationTest {

    @Test
    void logout_revokesRefreshToken_thenRefreshFails() throws Exception {
        var owner = registerOwner();

        logout(owner.accessToken(), owner.refreshToken()).andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(owner.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void logout_isIdempotent_secondCallStillSucceeds() throws Exception {
        var owner = registerOwner();

        logout(owner.accessToken(), owner.refreshToken()).andExpect(status().isNoContent());
        logout(owner.accessToken(), owner.refreshToken()).andExpect(status().isNoContent());
    }

    @Test
    void logout_accessTokenRemainsValidUntilItsOwnExpiry() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk());

        logout(owner.accessToken(), owner.refreshToken()).andExpect(status().isNoContent());

        // Same access token, no new login — logout only revokes the refresh token,
        // never bumps auth_version, so this must keep working until natural expiry.
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withoutAuthentication_returnsUnauthorized() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(owner.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withAnotherUsersRefreshToken_doesNotRevokeIt() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();

        // ownerA calls logout but supplies ownerB's refresh token — must not revoke it.
        logout(ownerA.accessToken(), ownerB.refreshToken()).andExpect(status().isNoContent());

        String secondLoginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(ownerB.email(), ownerB.password()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        objectMapper.readValue(secondLoginBody, AuthResponse.class);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(ownerB.refreshToken()))))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_alreadyRotatedToken_reusedAttemptReturnsUniform422() throws Exception {
        var owner = registerOwner();

        String firstRefreshBody = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(owner.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        objectMapper.readValue(firstRefreshBody, AuthResponse.class);

        // The original refresh token was rotated (revoked) by the call above — reusing
        // it must fail, not silently succeed a second time.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(owner.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions logout(String accessToken, String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))));
    }
}
