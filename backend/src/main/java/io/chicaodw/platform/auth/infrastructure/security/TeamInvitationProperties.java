package io.chicaodw.platform.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DT-017B — team invitation TTL, deliberately its own named property (not reused from
 * {@code OwnerInvite}'s hardcoded 7-day constant) so the two flows can be tuned
 * independently even though they start at the same default.
 */
@ConfigurationProperties(prefix = "app.team-invitation")
@Getter
@Setter
public class TeamInvitationProperties {

    /** 7 days, in seconds — same default duration as the owner-invite flow. */
    private long ttlSeconds = 604800;
}
