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
 */
@SpringBootTest
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
