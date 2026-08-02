package io.chicaodw.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers Sprint 11B.6D item 10 — confirms the security headers already configured in
 * SecurityConfig (explicitly) and applied by Spring Security's own defaults (CORS
 * filter aside) actually reach a real authentication response, not just the filter
 * chain configuration.
 */
@AutoConfigureMockMvc
class SecurityHeadersTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void authResponse_carriesTheFullSecurityHeaderSet() throws Exception {
        var request = new RegisterRequest(
                "Owner", "headers-" + System.nanoTime() + "@example.com",
                "securePass1", "Headers Test Co", "PT");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Security-Policy",
                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy",
                        containsString("camera=()")))
                // Spring Security's default header writer — never overridden, so an
                // authentication response is never cacheable by a browser/shared proxy.
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                // No HSTS: no real TLS termination in front of this app yet (Sprint 11C).
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}
