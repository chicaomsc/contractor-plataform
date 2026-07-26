package io.chicaodw.platform.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Backs PLATFORM_ADMIN_BOOTSTRAP_EMAIL/PASSWORD — see PlatformAdminBootstrapRunner. */
@ConfigurationProperties(prefix = "app.admin-bootstrap")
@Getter
@Setter
public class PlatformAdminBootstrapProperties {

    private String email;
    private String password;
}
