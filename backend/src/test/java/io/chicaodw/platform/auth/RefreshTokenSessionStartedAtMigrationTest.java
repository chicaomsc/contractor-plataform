package io.chicaodw.platform.auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DT-012 (Session Lifecycle, Phase A) — V15 must be safe to apply against a database
 * that already has {@code refresh_tokens} rows predating {@code session_started_at}:
 * this is exactly the production scenario the migration was written for (real
 * refresh_tokens rows already exist — see the migration's own comment header).
 *
 * Runs Flyway directly against its own throwaway container (not the one
 * {@link io.chicaodw.platform.AbstractIntegrationTest} shares across the rest of the
 * suite, which is always already fully migrated) so it can pause partway through the
 * migration history — right after V14, before V15 exists — and insert a pre-V15-shaped
 * row by hand, the way a real pre-DT-012 production row would look.
 */
@Testcontainers
class RefreshTokenSessionStartedAtMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void v15_backfillsSessionStartedAtFromCreatedAt_forRowsThatPredateTheColumn() throws SQLException {
        migrateTo("14");

        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        // Deliberately not "now" — a pre-existing row's own created_at is what the
        // migration's backfill must copy onto the new column.
        //
        // Truncated to microseconds: Postgres' timestamptz has a fixed 6-digit
        // (microsecond) fractional-seconds precision and ROUNDS on storage — confirmed
        // directly against a real postgres:17-alpine instance (information_schema
        // reports datetime_precision = 6). Instant.now() carries nanosecond precision,
        // so comparing an untouched Instant.now() value against what a round-trip
        // through the database produced was flaky: whenever the nanosecond remainder
        // happened to round the 6th fractional digit up (e.g. .534431966 -> .534432),
        // the raw in-memory value and the value read back after V15's backfill would
        // legitimately differ, even though the migration behaved correctly. Truncating
        // here — once, before the value ever reaches the database — makes the asserted
        // value bit-for-bit identical to what Postgres will actually persist and return,
        // independent of the wall-clock nanosecond noise on whatever machine runs this.
        Instant createdAt = Instant.now().minusSeconds(3600).truncatedTo(ChronoUnit.MICROS);

        try (Connection conn = connect()) {
            insertMinimalSuperAdminUser(conn, userId);
            insertPreV15RefreshToken(conn, tokenId, userId, createdAt);
        }

        migrateToLatest();

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT session_started_at FROM refresh_tokens WHERE id = ?")) {
            ps.setObject(1, tokenId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                Instant backfilled = rs.getTimestamp("session_started_at").toInstant();
                assertThat(backfilled).isEqualTo(createdAt);
            }
        }

        // The column must now be NOT NULL going forward — a new row omitting it must
        // fail, exactly like every other required column on this table.
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, revoked, created_at, updated_at)
                     VALUES (?, ?, ?, now() + interval '1 day', false, now(), now())
                     """)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, userId);
            ps.setString(3, "another-fake-hash-" + UUID.randomUUID());

            assertThatThrownBy(ps::execute).isInstanceOf(SQLException.class);
        }
    }

    private static void migrateTo(String targetVersion) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target(targetVersion)
                .load()
                .migrate();
    }

    private static void migrateToLatest() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    /** SUPER_ADMIN + NULL company_id satisfies chk_users_role_company_id (V11) without
     * needing a companies row too — the minimal user shape refresh_tokens' FK requires. */
    private static void insertMinimalSuperAdminUser(Connection conn, UUID userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, company_id, email, password_hash, name, role, status, auth_version, created_at, updated_at)
                VALUES (?, NULL, ?, 'irrelevant-hash', 'Migration Test User', 'SUPER_ADMIN', 'ACTIVE', 0, now(), now())
                """)) {
            ps.setObject(1, userId);
            ps.setString(2, "migration-test-" + userId + "@example.test");
            ps.execute();
        }
    }

    private static void insertPreV15RefreshToken(Connection conn, UUID tokenId, UUID userId, Instant createdAt)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, revoked, created_at, updated_at)
                VALUES (?, ?, ?, now() + interval '30 days', false, ?, ?)
                """)) {
            ps.setObject(1, tokenId);
            ps.setObject(2, userId);
            ps.setString(3, "pre-v15-fake-hash-" + tokenId);
            ps.setTimestamp(4, Timestamp.from(createdAt));
            ps.setTimestamp(5, Timestamp.from(createdAt));
            ps.execute();
        }
    }
}
