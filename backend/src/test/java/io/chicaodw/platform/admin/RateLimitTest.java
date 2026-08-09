package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.api.dto.AcceptInviteRequest;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordRequest;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.api.dto.ResetPasswordRequest;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-011B.5 §9 HARD-01 (SEC-AUTH-03) — in-memory rate limiting on exactly the five
 * authentication endpoints named in the DT, never a global limiter. Runs in its own
 * Spring context (distinct {@code @SpringBootTest(properties = ...)} from the shared
 * one used by every other test class) so its tight, test-only capacities can never
 * leak into — or be polluted by — the rest of the suite, which shares one loopback
 * remote address across hundreds of unrelated requests (see
 * {@code AbstractIntegrationTest}, which disables rate limiting by default for exactly
 * this reason).
 */
@SpringBootTest(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.register.capacity=3",
        "app.rate-limit.register.window-seconds=60",
        "app.rate-limit.login.capacity=3",
        "app.rate-limit.login.window-seconds=60",
        "app.rate-limit.refresh.capacity=3",
        "app.rate-limit.refresh.window-seconds=60",
        "app.rate-limit.forgot-password.capacity=3",
        "app.rate-limit.forgot-password.window-seconds=60",
        "app.rate-limit.reset-password.capacity=3",
        "app.rate-limit.reset-password.window-seconds=60",
        "app.rate-limit.invite-accept.capacity=3",
        "app.rate-limit.invite-accept.window-seconds=60",
        "app.rate-limit.admin-password-reset.capacity=3",
        "app.rate-limit.admin-password-reset.window-seconds=60",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitTest extends AbstractAdminIntegrationTest {

    private static final int ATTEMPTS = 5; // > capacity (3) regardless of any prior consumption in @BeforeAll

    @Autowired CompanyRepository companyRepository;

    private String superAdminToken;
    private UUID companyId;
    private UUID ownerId;

    /** Non-static + PER_CLASS so this can use the @Autowired mockMvc/objectMapper fields
     * (unavailable in a true static @BeforeAll) while still running exactly once, before
     * any @Test method — critical here, since the login-rate-limit test below
     * deliberately exhausts the very same /auth/login bucket this setup also uses. */
    @BeforeAll
    void setUpFixtures() throws Exception {
        superAdminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();
    }

    @Test
    void register_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new RegisterRequest("Rate Limit Owner", "rate-limit-register@example.com",
                                    "ValidPassword1", "Rate Limit Co", "PT"))));
        }
        assertTooManyRequests(last);
    }

    @Test
    void refresh_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new RefreshTokenRequest("bogus-refresh-token"))));
        }
        assertTooManyRequests(last);
    }

    @Test
    void login_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("nobody@example.com", "wrong-password"))));
        }
        assertTooManyRequests(last);
    }

    @Test
    void forgotPassword_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/auth/password/forgot")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("someone@example.com"))));
        }
        assertTooManyRequests(last);
    }

    @Test
    void resetPassword_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/auth/password/reset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ResetPasswordRequest("bogus-token", "NewPassword1"))));
        }
        assertTooManyRequests(last);
    }

    @Test
    void inviteAccept_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/auth/invites/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AcceptInviteRequest("bogus-token", "NewPassword1"))));
        }
        assertTooManyRequests(last);
    }

    @Test
    void adminPasswordReset_rateLimited_returns429AfterCapacityExceeded() throws Exception {
        ResultActions last = null;
        for (int i = 0; i < ATTEMPTS; i++) {
            last = mockMvc.perform(post("/admin/companies/" + companyId + "/owners/" + ownerId + "/password-reset")
                    .header("Authorization", "Bearer " + superAdminToken));
        }
        assertTooManyRequests(last);
    }

    private void assertTooManyRequests(ResultActions result) throws Exception {
        result.andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }
}
