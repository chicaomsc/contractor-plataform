package io.chicaodw.platform.auth;

import io.chicaodw.platform.auth.application.JwtService;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-junit-at-least-32-chars!!");
        props.setAccessTokenTtl(900);
        props.setRefreshTokenTtl(2592000);

        jwtService = new JwtService(props);

        // build a minimal User without JPA — only getters needed, set fields via reflection
        user = new User();
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setName("Alice");
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);

        UUID userId    = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id",        userId);
        ReflectionTestUtils.setField(user, "companyId", companyId);
    }

    @Test
    void shouldGenerateNonNullJwtString() {
        String token = jwtService.generateAccessToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldExtractCorrectSubject() {
        String token  = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    }

    @Test
    void shouldContainExpectedClaims() {
        String token  = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseClaims(token);
        assertThat(claims.get("email",     String.class)).isEqualTo("alice@example.com");
        assertThat(claims.get("role",      String.class)).isEqualTo("OWNER");
        assertThat(claims.get("companyId", String.class)).isEqualTo(user.getCompanyId().toString());
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("test-secret-key-for-junit-at-least-32-chars!!");
        shortLived.setAccessTokenTtl(-1); // already expired
        JwtService expiredService = new JwtService(shortLived);

        String token = expiredService.generateAccessToken(user);

        assertThatThrownBy(() -> expiredService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        String token    = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.parseClaims(tampered))
                .isInstanceOf(JwtException.class);
    }
}
