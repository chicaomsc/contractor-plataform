package io.chicaodw.platform.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DT-012 (Session Lifecycle, Phase A) — absolute session lifetime. Independent of the
 * access token TTL ({@link JwtProperties#getAccessTokenTtl()}) and the refresh token's
 * own TTL ({@link JwtProperties#getRefreshTokenTtl()}): a chain of rotated refresh
 * tokens may not extend a single login past this many seconds from the original
 * login/register/invite-accept, no matter how often it is refreshed. See
 * {@code RefreshToken#sessionStartedAt} and {@code AuthService#refresh}.
 */
@ConfigurationProperties(prefix = "app.session")
@Getter
@Setter
public class SessionProperties {

    private long absoluteLifetimeSeconds = 28800;
}
