package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.security.Keys;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("companyId", user.getCompanyId().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessTokenTtl() * 1_000L))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
