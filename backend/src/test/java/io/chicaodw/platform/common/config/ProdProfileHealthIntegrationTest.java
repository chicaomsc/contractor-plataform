package io.chicaodw.platform.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordRequest;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordResponse;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.common.email.EmailService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the "prod" profile end to end against a real Postgres (Testcontainers):
 * confirms the context actually boots with valid production-shaped config (proving
 * {@link ProductionReadinessValidator} accepts a good configuration, not just rejects
 * bad ones — see {@code ProductionReadinessValidatorTest} for the rejection cases),
 * and that the health/probe matcher in SecurityConfig is exactly as wide as intended:
 * health + its sub-paths public, nothing else in the Actuator namespace opened up.
 *
 * <p>Also the one place that proves {@code debugToken}/{@code debugResetLink} stay
 * suppressed under "prod" even with real email sending turned on — {@link EmailService}
 * is mocked here (never a real Resend call), but {@code app.email.enabled=true} and a
 * passing {@code ProductionReadinessValidator} check for it are both genuinely exercised.
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileHealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    EmailService emailService;

    @DynamicPropertySource
    static void prodProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> "test-only-strong-secret-for-prod-profile-tests-1234");
        registry.add("app.cors.allowed-origins", () -> "https://example.test");
        // Sprint 12.4.2 (RR-04/RR-05): ProductionReadinessValidator now also checks
        // these two — the application.yml defaults (localhost-based) would otherwise
        // fail context startup here, same as any real "prod" boot left unconfigured.
        registry.add("app.platform.base-domain", () -> "app.example.test");
        registry.add("app.platform.frontend-base-url", () -> "https://app.example.test");
        // Email delivery: enabled=true so ProductionReadinessValidator's email checks
        // (this feature) are genuinely exercised under "prod" too, not skipped.
        registry.add("app.email.enabled", () -> "true");
        registry.add("app.email.from", () -> "no-reply@example.test");
        registry.add("app.email.resend.api-key", () -> "re_test_key_not_real");
    }

    @Test
    void health_isPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("UP")));
    }

    @Test
    void liveness_isPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("UP")));
    }

    @Test
    void readiness_isPublicAndReflectsDatabaseAvailability() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("UP")));
    }

    @Test
    void otherActuatorEndpoints_remainProtected() throws Exception {
        // /actuator/info is NOT in the permitAll list — only health and its sub-paths
        // are. Falls through to .anyRequest().authenticated().
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_prodProfileWithEmailEnabled_stillNeverReturnsDebugFields_butStillSendsEmail() throws Exception {
        String email = "prod-reset-" + System.nanoTime() + "@example.test";
        User superAdmin = new User();
        superAdmin.setCompanyId(null);
        superAdmin.setEmail(email);
        superAdmin.setPasswordHash(passwordEncoder.encode("SuperAdminPass1"));
        superAdmin.setName("Prod Profile Test Admin");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);
        userRepository.save(superAdmin);

        String body = mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ForgotPasswordResponse response = objectMapper.readValue(body, ForgotPasswordResponse.class);
        assertThat(response.debugToken()).isNull();
        assertThat(response.debugResetLink()).isNull();

        Mockito.verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq(email), any(), any(Duration.class));
    }
}
