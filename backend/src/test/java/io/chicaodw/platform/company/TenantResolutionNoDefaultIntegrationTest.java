package io.chicaodw.platform.company;

import io.chicaodw.platform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Separate context from TenantResolutionIntegrationTest: no fallback configured at all. */
@AutoConfigureMockMvc
class TenantResolutionNoDefaultIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void tenantProperties(DynamicPropertyRegistry registry) {
        registry.add("app.platform.base-domain", () -> "localhost");
        registry.add("app.platform.default-tenant-slug", () -> "");
    }

    @Test
    void unknownHost_withNoFallbackConfigured_returnsNotFound() throws Exception {
        mockMvc.perform(get("/public/tenant").header("Host", "totally-unknown-host.example.com"))
                .andExpect(status().isNotFound());
    }
}
