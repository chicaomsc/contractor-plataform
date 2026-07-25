package io.chicaodw.platform.common.config;

import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.common.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionReadinessValidatorTest {

    private static final String VALID_SECRET = "a-valid-strong-test-secret-with-32-plus-bytes!!";
    private static final String VALID_CORS = "https://app.example.pt";

    @Mock
    Environment environment;

    JwtProperties jwtProperties;
    StorageProperties storageProperties;
    Path writableStorageDir;

    ProductionReadinessValidator validator;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(VALID_SECRET);

        writableStorageDir = tempDir.resolve("storage");
        storageProperties = new StorageProperties();
        storageProperties.setBasePath(writableStorageDir.toString());

        validator = new ProductionReadinessValidator(environment, jwtProperties, storageProperties);
    }

    private void activeProfiles(String... profiles) {
        when(environment.getActiveProfiles()).thenReturn(profiles);
    }

    // ── Profile gating ───────────────────────────────────────────────────────

    @Test
    void run_noProfileActive_isNoOp_evenWithInvalidConfig() {
        activeProfiles();
        jwtProperties.setSecret("");

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_localProfileActive_isNoOp_evenWithInvalidConfig() {
        activeProfiles("local");
        jwtProperties.setSecret("too-short");

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_prodProfileActive_withValidConfig_doesNotThrow() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        Files.createDirectories(writableStorageDir);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    // ── JWT_SECRET ───────────────────────────────────────────────────────────

    @Test
    void run_prodWithMissingSecret_throws() {
        activeProfiles("prod");
        jwtProperties.setSecret(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing");
    }

    @Test
    void run_prodWithBlankSecret_throws() {
        activeProfiles("prod");
        jwtProperties.setSecret("   ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing");
    }

    @Test
    void run_prodWithTooShortSecret_throws() {
        activeProfiles("prod");
        jwtProperties.setSecret("short-secret");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void run_prodWithDevFallbackPlaceholder_throws() {
        activeProfiles("prod");
        jwtProperties.setSecret("dev-only-secret-at-least-32-chars-change-in-prod!");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("known placeholder");
    }

    @Test
    void run_prodWithExampleEnvPlaceholder_throws() {
        activeProfiles("prod");
        jwtProperties.setSecret("CHANGE_ME_WITH_A_STRONG_SECRET_MIN_32_CHARS");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("known placeholder");
    }

    @Test
    void run_secretRejection_neverLeaksTheSecretValue() {
        activeProfiles("prod");
        String secretThatMustNotLeak = "super-secret-value-that-must-never-appear-in-logs";
        jwtProperties.setSecret(secretThatMustNotLeak);
        // valid length (>= 32 bytes) but not a known placeholder — so it fails the
        // *next* check (CORS, since it's unset here), proving the JWT value itself
        // was accepted and never had a reason to be printed anywhere on this path.
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(secretThatMustNotLeak);
    }

    // ── APP_CORS_ALLOWED_ORIGINS ─────────────────────────────────────────────

    @Test
    void run_prodWithMissingCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_CORS_ALLOWED_ORIGINS is required");
    }

    @Test
    void run_prodWithBlankCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("   ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_CORS_ALLOWED_ORIGINS is required");
    }

    @Test
    void run_prodWithWildcardCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("*");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    void run_prodWithStrayCommaProducingBlankEntry_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins"))
                .thenReturn("https://app.example.pt,,https://www.app.example.pt");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blank entry");
    }

    @Test
    void run_prodWithNonUrlCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("not-a-url");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_prodWithNonHttpSchemeCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("ftp://app.example.pt");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void run_prodWithLocalhostCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("http://localhost:3000");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void run_prodWithLoopbackIpCors_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("http://127.0.0.1:3000");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void run_prodWithValidMultipleOrigins_trimmed_doesNotThrow() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins"))
                .thenReturn(" https://app.example.pt , https://www.app.example.pt ");
        Files.createDirectories(writableStorageDir);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    // ── STORAGE_PATH ─────────────────────────────────────────────────────────

    @Test
    void run_prodWithCreatableStorageDir_createsItAndDoesNotThrow() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);

        assertThat(Files.exists(writableStorageDir)).isFalse();
        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
        assertThat(Files.isDirectory(writableStorageDir)).isTrue();
    }

    @Test
    void run_prodWithNonWritableStorageDir_throws() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);

        // Read+execute only, no write — POSIX permissions. This assumes a non-root
        // test runner (root ignores file permission bits); GitHub Actions'
        // ubuntu-latest runners already satisfy this for the rest of this suite.
        Files.createDirectory(writableStorageDir,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr-xr-x")));

        try {
            assertThatThrownBy(() -> validator.run(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not writable");
        } finally {
            // Restore write permission so @TempDir cleanup can delete the directory.
            Files.setPosixFilePermissions(writableStorageDir, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    void run_prodWithStorageDirThatIsActuallyAFile_throws() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);

        Files.createFile(writableStorageDir);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a directory");
    }

    @Test
    void run_neverDeletesExistingFilesInStorageDir() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);

        Files.createDirectories(writableStorageDir);
        Path existingFile = writableStorageDir.resolve("logo.png");
        Files.writeString(existingFile, "not-really-a-png-but-that-is-fine-for-this-test");

        validator.run(null);

        assertThat(Files.exists(existingFile)).isTrue();
    }
}
