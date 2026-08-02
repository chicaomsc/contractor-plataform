package io.chicaodw.platform;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base for all integration tests.
 *
 * The PostgreSQL container is started once (static initializer) and reused across
 * all subclasses in the same JVM run. Ryuk handles cleanup on JVM exit.
 *
 * Not using @Testcontainers + @Container avoids the container being stopped by the
 * JUnit 5 extension's afterAll between test classes, which would break subsequent
 * test classes that share the same cached Spring application context.
 *
 * Rate limiting (AuthRateLimitFilter, Sprint 11B.6A) is disabled by default here: its
 * in-memory buckets are keyed by remote address + path, and every MockMvc request in
 * this shared context reports the same loopback address — without this override, the
 * cumulative login/forgot/reset/invite-accept calls made by unrelated tests across the
 * whole suite would eventually trip the limiter and fail tests that have nothing to do
 * with rate limiting. Dedicated rate-limit tests re-enable it with their own tight
 * limits via their own @SpringBootTest(properties = ...), which gets its own context.
 */
@SpringBootTest(properties = "app.rate-limit.enabled=false")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureTestDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
