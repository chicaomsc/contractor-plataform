package io.chicaodw.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.common.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-012 (Session Lifecycle, Phase A) — absolute session lifetime, enforced by
 * {@code AuthService.refresh()} via the atomic
 * {@code RefreshTokenRepository.markRevokedIfStillValid} UPDATE. The default lifetime
 * is 8h (app.session.absolute-lifetime-seconds); rather than waiting 8h in a test, each
 * scenario here manipulates the persisted {@code session_started_at} directly (via
 * RefreshTokenRepository, bypassing the service layer) to simulate a session that
 * started at a known point in the past — the same technique already used elsewhere in
 * this suite for expires_at (see PasswordResetFlowTest).
 */
@AutoConfigureMockMvc
class AbsoluteSessionLifetimeIntegrationTest extends AbstractIntegrationTest {

    private static final long ABSOLUTE_LIFETIME_SECONDS = 28800; // 8h — application.yml default

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private String email;
    private static final String PASSWORD = "securePass1";

    @BeforeEach
    void registerUser() {
        email = "abs-lifetime-test-" + System.nanoTime() + "@example.com";
    }

    private AuthResponse register() throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", email, PASSWORD, "Test Corp", "PT");
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class);
    }

    private AuthResponse refresh(String refreshToken) throws Exception {
        String body = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class);
    }

    private RefreshToken storedToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken)).orElseThrow();
    }

    /** Directly rewrites session_started_at on the persisted row — simulates "this
     * session actually began `secondsAgo` seconds ago" without waiting in real time. */
    private void backdateSessionStart(String rawToken, long secondsAgo) {
        RefreshToken token = storedToken(rawToken);
        token.setSessionStartedAt(Instant.now().minusSeconds(secondsAgo));
        refreshTokenRepository.save(token);
    }

    // ── 1. login creates a refresh token carrying sessionStartedAt ─────────────

    @Test
    void register_createsRefreshToken_withSessionStartedAt() throws Exception {
        AuthResponse auth = register();

        RefreshToken stored = storedToken(auth.refreshToken());

        assertThat(stored.getSessionStartedAt()).isNotNull();
        assertThat(stored.getSessionStartedAt()).isCloseTo(Instant.now(), within3Seconds());
    }

    // ── 2. refresh well before 8h works and preserves the original sessionStartedAt ──

    @Test
    void refresh_wellWithinAbsoluteLifetime_succeedsAndPreservesOriginalSessionStartedAt() throws Exception {
        AuthResponse auth = register();
        Instant original = storedToken(auth.refreshToken()).getSessionStartedAt();
        // Simulate: this session actually started 1h ago — still well within 8h.
        backdateSessionStart(auth.refreshToken(), 3600);
        Instant backdatedOriginal = storedToken(auth.refreshToken()).getSessionStartedAt();

        AuthResponse refreshed = refresh(auth.refreshToken());

        RefreshToken newToken = storedToken(refreshed.refreshToken());
        assertThat(newToken.getSessionStartedAt()).isEqualTo(backdatedOriginal);
        assertThat(newToken.getSessionStartedAt()).isNotEqualTo(original); // sanity: backdate actually took effect
    }

    // ── 3. multiple rotations all preserve exactly the original sessionStartedAt ────

    @Test
    void multipleRotations_allPreserveExactlyTheOriginalSessionStartedAt() throws Exception {
        AuthResponse t0 = register();
        // T0 — 30 minutes into the session (well within 8h), so three more rotations
        // below still land comfortably inside the absolute lifetime.
        backdateSessionStart(t0.refreshToken(), 1800);
        Instant sessionStart = storedToken(t0.refreshToken()).getSessionStartedAt();

        AuthResponse t1 = refresh(t0.refreshToken());
        assertThat(storedToken(t1.refreshToken()).getSessionStartedAt()).isEqualTo(sessionStart);

        AuthResponse t2 = refresh(t1.refreshToken());
        assertThat(storedToken(t2.refreshToken()).getSessionStartedAt()).isEqualTo(sessionStart);

        AuthResponse t3 = refresh(t2.refreshToken());
        assertThat(storedToken(t3.refreshToken()).getSessionStartedAt()).isEqualTo(sessionStart);
    }

    // ── 4. absolute lifetime exceeded — refresh rejected, no new credential issued ──

    @Test
    void refresh_absoluteLifetimeExceeded_isRejected_andIssuesNoNewCredential() throws Exception {
        AuthResponse auth = register();
        // Session "started" 8h + 5s ago — past the 8h absolute lifetime — even though
        // this specific refresh token's own 30-day TTL has barely been touched.
        backdateSessionStart(auth.refreshToken(), ABSOLUTE_LIFETIME_SECONDS + 5);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(auth.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());

        // The presented token itself must not have been consumed/revoked by the failed
        // attempt — this mirrors "no side effect on failure", same as an unknown token.
        RefreshToken stillThere = storedToken(auth.refreshToken());
        assertThat(stillThere.isRevoked()).isFalse();
    }

    // ── 5. boundary — right at the 8h edge ──────────────────────────────────────

    @Test
    void refresh_justInsideAbsoluteLifetime_succeeds() throws Exception {
        AuthResponse auth = register();
        // 5s of slack before the 8h cutoff — well inside, but as close to the edge as a
        // real HTTP round-trip can reliably land without flaking on CI timing jitter.
        backdateSessionStart(auth.refreshToken(), ABSOLUTE_LIFETIME_SECONDS - 5);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(auth.refreshToken()))))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_justPastAbsoluteLifetime_isRejected() throws Exception {
        AuthResponse auth = register();
        backdateSessionStart(auth.refreshToken(), ABSOLUTE_LIFETIME_SECONDS + 5);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(auth.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());
    }

    private static org.assertj.core.data.TemporalUnitOffset within3Seconds() {
        return org.assertj.core.api.Assertions.within(3, java.time.temporal.ChronoUnit.SECONDS);
    }
}
