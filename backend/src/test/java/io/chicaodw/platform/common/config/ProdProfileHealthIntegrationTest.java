package io.chicaodw.platform.common.config;

import io.chicaodw.platform.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the "prod" profile end to end against a real Postgres (Testcontainers):
 * confirms the context actually boots with valid production-shaped config (proving
 * {@link ProductionReadinessValidator} accepts a good configuration, not just rejects
 * bad ones — see {@code ProductionReadinessValidatorTest} for the rejection cases),
 * and that the health/probe matcher in SecurityConfig is exactly as wide as intended:
 * health + its sub-paths public, nothing else in the Actuator namespace opened up.
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileHealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void prodProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> "test-only-strong-secret-for-prod-profile-tests-1234");
        registry.add("app.cors.allowed-origins", () -> "https://example.test");
        // Sprint 12.4.2 (RR-04/RR-05): ProductionReadinessValidator now also checks
        // these two — the application.yml defaults (localhost-based) would otherwise
        // fail context startup here, same as any real "prod" boot left unconfigured.
        registry.add("app.platform.base-domain", () -> "app.example.test");
        registry.add("app.platform.frontend-base-url", () -> "https://app.example.test");
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
}
