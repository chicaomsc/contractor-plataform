package io.chicaodw.platform.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * In-memory rate limiting for the authentication endpoints only (Sprint 11B.6A,
 * SEC-AUTH-03) — never a global limiter. Each endpoint has its own capacity/window,
 * keyed by the caller's remote address (see {@link AuthRateLimitFilter}).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private Rule login = new Rule(10, 60);
    private Rule forgotPassword = new Rule(5, 60);
    private Rule resetPassword = new Rule(10, 60);
    private Rule inviteAccept = new Rule(10, 60);
    private Rule adminPasswordReset = new Rule(10, 60);
    // Sprint 12.4.2 (RR-06) — self-serve account/company creation, no auth required to
    // call it: the most valuable endpoint to throttle tightly. 5/hour per remote address
    // is well above one legitimate signup, low enough to blunt scripted spam.
    private Rule register = new Rule(5, 3600);
    // Sprint 12.4.2 (RR-07) — requires an already-valid refresh token, so the abuse
    // surface is smaller than register's, but it was the only POST /auth/** endpoint
    // with zero coverage. 20/hour comfortably covers legitimate traffic (access tokens
    // expire every 15 minutes, app.jwt.access-token-ttl — a single active session needs
    // at most 4/hour; 20 leaves headroom for multiple tabs/devices) while still bounding
    // sustained abuse of a leaked/guessed token.
    private Rule refresh = new Rule(20, 3600);

    @Getter
    @Setter
    public static class Rule {

        private int capacity;
        private long windowSeconds;

        public Rule() {
        }

        public Rule(int capacity, long windowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
        }
    }
}
