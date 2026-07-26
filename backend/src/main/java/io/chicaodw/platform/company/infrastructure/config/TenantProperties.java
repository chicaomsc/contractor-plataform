package io.chicaodw.platform.company.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime multi-tenant resolution config (DT-011A.7 §16). {@code defaultTenantSlug} is
 * an explicit, temporary transition mechanism — see DT §22/§24 — not a permanent
 * architecture; it exists only because production has no real subdomain yet.
 */
@ConfigurationProperties(prefix = "app.platform")
@Getter
@Setter
public class TenantProperties {

    private String baseDomain = "localhost";
    private String defaultTenantSlug;
}
