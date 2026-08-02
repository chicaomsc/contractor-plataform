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
