package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies access tokens. Every claim below is deliberately checked on
 * every parse, not just present at issuance (Sprint 11B.6D, JWT hardening):
 * issuer/audience are required (a token minted for a different deployment is
 * rejected outright), the signing algorithm is fixed to HS256 and {@link
 * #signingKey()} is only ever handed to {@code verifyWith}, which restricts
 * acceptable signatures to the HMAC family — an RS/ES/PS-signed token, or an
 * unsigned ("none" algorithm) token, is never accepted by {@code parseSignedClaims}
 * regardless of its header. Rotation with a {@code kid} header is intentionally not
 * implemented yet (SEC-AUTH-11 remains informational/architectural) — see
 * docs/design/DT-011B.2 for the documented future direction.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("authVersion", user.getAuthVersion());
        // SUPER_ADMIN has no company — the claim is simply omitted rather than
        // encoding a sentinel value (see DT-011A.7 §5).
        if (user.getCompanyId() != null) {
            builder.claim("companyId", user.getCompanyId().toString());
        }
        return builder
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessTokenTtl() * 1_000L))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        JwtParserBuilder parser = Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .clockSkewSeconds(Duration.ofSeconds(jwtProperties.getClockSkewSeconds()).toSeconds());
        return parser.build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
