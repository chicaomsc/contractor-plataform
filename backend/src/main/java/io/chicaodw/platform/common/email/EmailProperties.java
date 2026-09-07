package io.chicaodw.platform.common.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code enabled} defaults to {@code false} deliberately — local/dev/test environments
 * never send real email unless someone explicitly opts in, and a fresh production
 * deployment that hasn't configured Resend yet still boots normally (password reset
 * keeps working exactly as before: admin-assisted link delivery, no regression). See
 * {@code ProductionReadinessValidator} for the checks that apply once this is turned on.
 */
@ConfigurationProperties(prefix = "app.email")
@Getter
@Setter
public class EmailProperties {

    private boolean enabled = false;
    private String from = "";
    private Resend resend = new Resend();

    @Getter
    @Setter
    public static class Resend {
        private String apiKey = "";
    }
}
