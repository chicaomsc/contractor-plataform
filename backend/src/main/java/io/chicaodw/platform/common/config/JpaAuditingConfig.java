package io.chicaodw.platform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // Populates createdAt / updatedAt via @CreatedDate / @LastModifiedDate on BaseEntity.
    // createdBy / updatedBy will be added after auth is implemented.
}
