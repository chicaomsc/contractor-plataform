package io.chicaodw.platform.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11B.6D item 2 — BCrypt strength is configurable via
 * {@code app.security.bcrypt-strength}, wired into {@code SecurityConfig.bcryptStrength}
 * by Spring's {@code @Value}. This test bypasses the container (sets the field
 * directly) and asserts against the produced hash's own cost prefix — BCrypt encodes
 * its cost factor in the hash string itself ({@code $2a$<cost>$...}), so this is a
 * real behavioral check, not just a constructor-argument check.
 */
class SecurityConfigPasswordEncoderTest {

    @Test
    void passwordEncoder_usesConfiguredStrength() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "bcryptStrength", 6);

        PasswordEncoder encoder = config.passwordEncoder();
        String hash = encoder.encode("some-password");

        assertThat(hash).startsWith("$2a$06$");
        assertThat(encoder.matches("some-password", hash)).isTrue();
    }

    @Test
    void passwordEncoder_defaultConfiguredStrengthIsTwelve() {
        // Mirrors application.yml's app.security.bcrypt-strength default — SEC-AUTH-10.
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "bcryptStrength", 12);

        PasswordEncoder encoder = config.passwordEncoder();
        String hash = encoder.encode("some-password");

        assertThat(hash).startsWith("$2a$12$");
    }
}
