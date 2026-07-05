package io.chicaodw.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoads() {
        // Spring context loads → Flyway migrations were applied successfully
    }

    @Test
    void flywayAppliedV1AndV2() {
        // Verify both migrations ran: uuid-ossp extension + 4 domain tables
        var tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename",
                String.class
        );
        assertThat(tables).containsExactlyInAnyOrder(
                "brandings", "companies", "flyway_schema_history",
                "refresh_tokens", "settings", "users"
        );
    }
}
