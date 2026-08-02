package io.chicaodw.platform.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessTokenTtl = 900;
    private long refreshTokenTtl = 2592000;

    // Sprint 11B.6D (SEC-AUTH-11 readiness) — issued and required on every parse so a
    // token minted by a different deployment/environment (same secret reused by
    // mistake, or a future multi-issuer setup) is rejected outright rather than only
    // relying on signature validity.
    private String issuer = "contractor-platform";
    private String audience = "contractor-platform-api";

    // Small and deliberately non-zero: absorbs realistic clock drift between app
    // instances without meaningfully extending a token's effective lifetime.
    private long clockSkewSeconds = 30;
}
