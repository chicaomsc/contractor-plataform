package io.chicaodw.platform.auth;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.chicaodw.platform.auth.application.JwtService;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

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
        assertThat(claims.get("authVersion", Long.class)).isEqualTo(0L);
    }

    // ── authVersion (DT-011A.10 §5) ─────────────────────────────────────────────

    @Test
    void shouldContainCurrentAuthVersion_whenNonZero() {
        user.setAuthVersion(3);

        String token  = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.get("authVersion", Long.class)).isEqualTo(3L);
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("test-secret-key-for-junit-at-least-32-chars!!");
        // Well beyond the configured clock-skew tolerance (default 30s) — a token
        // that "just" expired within the skew window is deliberately still accepted
        // (see shouldAcceptTokenWithinConfiguredClockSkew), so this must expire by more.
        shortLived.setAccessTokenTtl(-3600);
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

    // ── issuer / audience / algorithm hardening (Sprint 11B.6D) ────────────────

    @Test
    void generatedToken_carriesConfiguredIssuerAndAudience() {
        String token  = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getIssuer()).isEqualTo("contractor-platform");
        assertThat(claims.getAudience()).containsExactly("contractor-platform-api");
    }

    @Test
    void shouldRejectTokenWithUnexpectedIssuer() {
        JwtProperties otherIssuer = new JwtProperties();
        otherIssuer.setSecret("test-secret-key-for-junit-at-least-32-chars!!");
        otherIssuer.setIssuer("some-other-deployment");
        JwtService otherIssuerService = new JwtService(otherIssuer);

        String token = otherIssuerService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWithUnexpectedAudience() {
        JwtProperties otherAudience = new JwtProperties();
        otherAudience.setSecret("test-secret-key-for-junit-at-least-32-chars!!");
        otherAudience.setAudience("some-other-api");
        JwtService otherAudienceService = new JwtService(otherAudience);

        String token = otherAudienceService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithADifferentSecret_algorithmConfusionGuard() {
        // Built by hand with jjwt directly (not via JwtService) — a token forged with
        // a different key entirely, simulating the classic "attacker knows the
        // algorithm shape but not the secret" scenario. verifyWith(SecretKey) also
        // means an RS/ES/PS-signed or unsigned ("none") token is never accepted here
        // regardless of secret, since parseSignedClaims requires a JWS matching a
        // symmetric key's algorithm family.
        var wrongKey = Keys.hmacShaKeyFor("a-completely-different-secret-key-32b!!".getBytes());
        long now = System.currentTimeMillis();
        String forged = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("contractor-platform")
                .audience().add("contractor-platform-api").and()
                .claim("role", "SUPER_ADMIN")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 900_000L))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void shouldRejectTokenSignedWithHs384() {
        var hs384Key = Jwts.SIG.HS384.key().build();

        long now = System.currentTimeMillis();

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("contractor-platform")
                .audience().add("contractor-platform-api").and()
                .claim("role", user.getRole().name())
                .claim("authVersion", user.getAuthVersion())
                .issuedAt(new Date(now))
                .expiration(new Date(now + 900_000L))
                .signWith(hs384Key, Jwts.SIG.HS384)
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldAcceptTokenWithinConfiguredClockSkew() {
        JwtProperties futureIssued = new JwtProperties();
        futureIssued.setSecret("test-secret-key-for-junit-at-least-32-chars!!");
        futureIssued.setClockSkewSeconds(30);
        JwtService futureIssuedService = new JwtService(futureIssued);

        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("contractor-platform")
                .audience().add("contractor-platform-api").and()
                .issuedAt(new Date(now))
                // "expires" 10s ago — inside the 30s configured skew, so still accepted.
                .expiration(new Date(now - 10_000L))
                .signWith(Keys.hmacShaKeyFor("test-secret-key-for-junit-at-least-32-chars!!".getBytes()), Jwts.SIG.HS256)
                .compact();

        assertThat(futureIssuedService.parseClaims(token)).isNotNull();
    }

    // ── SUPER_ADMIN (no company) — DT-011A.7 §5/§13 ────────────────────────────

    @Test
    void shouldGenerateTokenForSuperAdminWithoutThrowing() {
        User superAdmin = new User();
        superAdmin.setEmail("admin@example.com");
        superAdmin.setPasswordHash("hash");
        superAdmin.setName("Platform Admin");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(superAdmin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(superAdmin, "companyId", null);

        String token = jwtService.generateAccessToken(superAdmin);

        assertThat(token).isNotBlank();
    }

    @Test
    void superAdminToken_omitsCompanyIdClaimEntirely() {
        User superAdmin = new User();
        superAdmin.setEmail("admin@example.com");
        superAdmin.setPasswordHash("hash");
        superAdmin.setName("Platform Admin");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(superAdmin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(superAdmin, "companyId", null);

        String token  = jwtService.generateAccessToken(superAdmin);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.get("companyId")).isNull();
        assertThat(claims.get("role", String.class)).isEqualTo("SUPER_ADMIN");
    }
}
