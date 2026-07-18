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
    void flywayAppliedAllMigrations() {
        // Verify all migrations ran: uuid-ossp extension + domain tables through V8
        var tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename",
                String.class
        );
        assertThat(tables).containsExactlyInAnyOrder(
                "brandings", "companies", "customers", "estimate_items", "estimate_number_sequences",
                "estimate_shares", "estimates", "flyway_schema_history", "gallery_items", "materials",
                "refresh_tokens", "services", "settings", "users"
        );
    }
}
