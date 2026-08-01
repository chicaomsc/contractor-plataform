package io.chicaodw.platform.company.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime multi-tenant resolution config (DT-011A.7 §16). {@code defaultTenantSlug} is
 * an explicit, temporary transition mechanism — see DT §22/§24 — not a permanent
 * architecture; it exists only because production has no real subdomain yet.
 *
 * {@code frontendBaseUrl} is unrelated to tenant resolution — it's the platform-wide
 * frontend origin used to build absolute links the backend hands out directly (today:
 * the password-reset link, DT-011A.10 §3/§13). Added here rather than a new properties
 * class because it belongs to the same "platform-wide, non-tenant-specific" config
 * group as {@code baseDomain} (see DT-011A.8 §on NEXT_PUBLIC_PLATFORM_BASE_DOMAIN for
 * the frontend-side precedent of this grouping).
 */
@ConfigurationProperties(prefix = "app.platform")
@Getter
@Setter
public class TenantProperties {

    private String baseDomain = "localhost";
    private String defaultTenantSlug;
    private String frontendBaseUrl = "http://localhost:3000";
}
