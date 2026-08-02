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
        // Verify all migrations ran: uuid-ossp extension + domain tables through V14
        // (Sprint 11A.7 added owner_invites via V11/V12; Sprint 11A.10 added
        // password_reset_tokens via V13; Sprint 11B.6A hashed refresh_tokens via V14 —
        // same table set, no new/removed table).
        var tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename",
                String.class
        );
        assertThat(tables).containsExactlyInAnyOrder(
                "brandings", "companies", "customers", "estimate_items", "estimate_number_sequences",
                "estimate_shares", "estimates", "flyway_schema_history", "gallery_items", "materials",
                "owner_invites", "password_reset_tokens", "refresh_tokens", "services", "settings", "users"
        );
    }

    @Test
    void flywayAppliedExactlyFourteenMigrations() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(count).isEqualTo(14);
    }
}
