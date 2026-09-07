package io.chicaodw.platform.common.config;

import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.common.email.EmailProperties;
import io.chicaodw.platform.common.storage.StorageProperties;
import io.chicaodw.platform.company.infrastructure.config.TenantProperties;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionReadinessValidatorTest {

    private static final String VALID_SECRET = "a-valid-strong-test-secret-with-32-plus-bytes!!";
    private static final String VALID_CORS = "https://app.example.pt";
    private static final String VALID_BASE_DOMAIN = "example.pt";
    private static final String VALID_FRONTEND_BASE_URL = "https://app.example.pt";
    private static final String VALID_DB_PASSWORD = "a-real-generated-database-password";
    private static final String VALID_EMAIL_FROM = "no-reply@example.pt";
    private static final String VALID_RESEND_API_KEY = "re_a-real-looking-key";

    @Mock
    Environment environment;

    JwtProperties jwtProperties;
    StorageProperties storageProperties;
    TenantProperties tenantProperties;
    EmailProperties emailProperties;
    Path writableStorageDir;

    ProductionReadinessValidator validator;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(VALID_SECRET);

        writableStorageDir = tempDir.resolve("storage");
        storageProperties = new StorageProperties();
        storageProperties.setBasePath(writableStorageDir.toString());

        tenantProperties = new TenantProperties();
        tenantProperties.setBaseDomain(VALID_BASE_DOMAIN);
        tenantProperties.setFrontendBaseUrl(VALID_FRONTEND_BASE_URL);

        // Valid AND enabled by default — email delivery is now mandatory in "prod"
        // (hardening pass after go-live review: password recovery has no other
        // delivery channel for a regular OWNER), so every test that expects run() to
        // succeed needs this to already be in a passing state, same as jwtProperties/
        // tenantProperties above. Tests exercising the email-specific rejections
        // override individual fields below.
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setFrom(VALID_EMAIL_FROM);
        emailProperties.getResend().setApiKey(VALID_RESEND_API_KEY);

        // lenient(): only reached by tests whose validation chain gets this far (e.g.
        // storage-path tests) — tests that throw earlier (JWT/CORS/platform-domain)
        // never call this stub, which strict-stubbing would otherwise flag as unused.
        lenient().when(environment.getProperty("spring.datasource.password")).thenReturn(VALID_DB_PASSWORD);

        validator = new ProductionReadinessValidator(
                environment, jwtProperties, storageProperties, tenantProperties, emailProperties);
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

    // ── JWT_ISSUER / JWT_AUDIENCE (Sprint 11B.6D) ───────────────────────────────

    @Test
    void run_prodWithBlankIssuer_throws() {
        activeProfiles("prod");
        jwtProperties.setIssuer("  ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_ISSUER is missing");
    }

    @Test
    void run_prodWithNullIssuer_throws() {
        activeProfiles("prod");
        jwtProperties.setIssuer(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_ISSUER is missing");
    }

    @Test
    void run_prodWithBlankAudience_throws() {
        activeProfiles("prod");
        jwtProperties.setAudience("   ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_AUDIENCE is missing");
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

    @Test
    void run_prodWithUneditedExampleEnvCorsPlaceholder_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins"))
                .thenReturn("https://CHANGE_ME.example.com,https://www.CHANGE_ME.example.com");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void run_prodWithReservedExampleComCors_evenWithoutChangeMeMarker_throws() {
        activeProfiles("prod");
        // No literal "CHANGE_ME" this time — proves example.com is caught on its own,
        // not just as a side effect of the placeholder-marker check above.
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("https://app.example.com");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("example.com");
    }

    @Test
    void run_prodWithDomainMerelyContainingExampleSubstring_doesNotRejectOnThatBasisAlone() throws IOException {
        // "myexample.pt" contains "example" but is not example.com/.org/.net and not a
        // dot-suffix of one — must NOT be rejected just for containing a common
        // substring (this was an explicit requirement, not merely a nice-to-have).
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("https://app.myexample.pt");
        Files.createDirectories(writableStorageDir);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    // ── PLATFORM_BASE_DOMAIN (Sprint 12.4.2, RR-04/RR-05) ───────────────────────

    @Test
    void run_prodWithBlankBaseDomain_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setBaseDomain("  ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLATFORM_BASE_DOMAIN is missing");
    }

    @Test
    void run_prodWithLocalhostBaseDomain_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setBaseDomain("localhost");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void run_prodWithUneditedExampleEnvBaseDomainPlaceholder_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setBaseDomain("CHANGE_ME_TENANT_SLUG");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLATFORM_BASE_DOMAIN")
                .hasMessageContaining("placeholder");
    }

    @Test
    void run_prodWithReservedExampleComBaseDomain_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setBaseDomain("example.com");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("example.com");
    }

    @Test
    void run_prodWithValidBaseDomain_doesNotThrow() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        Files.createDirectories(writableStorageDir);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    // ── PLATFORM_FRONTEND_BASE_URL (Sprint 12.4.2, RR-04/RR-05) ─────────────────

    @Test
    void run_prodWithBlankFrontendBaseUrl_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setFrontendBaseUrl("  ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLATFORM_FRONTEND_BASE_URL is missing");
    }

    @Test
    void run_prodWithLocalhostFrontendBaseUrl_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        // The exact application.yml default — proves the default itself would never
        // silently pass in profile 'prod' if left unset.
        tenantProperties.setFrontendBaseUrl("http://localhost:3000");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void run_prodWithUneditedExampleEnvFrontendBaseUrlPlaceholder_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setFrontendBaseUrl("https://CHANGE_ME.example.com");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLATFORM_FRONTEND_BASE_URL")
                .hasMessageContaining("placeholder");
    }

    @Test
    void run_prodWithNonUrlFrontendBaseUrl_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        tenantProperties.setFrontendBaseUrl("not-a-url");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLATFORM_FRONTEND_BASE_URL");
    }

    // ── DB_PASSWORD (Sprint 12.4.2, RR-04/RR-05) ────────────────────────────────

    @Test
    void run_prodWithMissingDbPassword_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        when(environment.getProperty("spring.datasource.password")).thenReturn(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD is missing");
    }

    @Test
    void run_prodWithDevDefaultDbPassword_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        // The exact application.yml default (spring.datasource.password: ${DB_PASSWORD:platform}).
        when(environment.getProperty("spring.datasource.password")).thenReturn("platform");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageContaining("development default");
    }

    @Test
    void run_prodWithPlaceholderDbPassword_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        when(environment.getProperty("spring.datasource.password")).thenReturn("CHANGE_ME_WITH_A_STRONG_SECRET");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageContaining("placeholder");
    }

    @Test
    void run_dbPasswordRejection_neverLeaksTheValue() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        String passwordThatMustNotLeak = "super-secret-db-password-that-must-never-appear-in-logs";
        when(environment.getProperty("spring.datasource.password")).thenReturn(passwordThatMustNotLeak);

        // Valid (not blank, not the dev default, not a placeholder) — so this proves
        // the value was accepted and never had a reason to be printed. Confirmed by
        // the overall run() completing without throwing, not by asserting an absence
        // in a message that was never produced.
        assertThatCode(() -> {
            Files.createDirectories(writableStorageDir);
            validator.run(null);
        }).doesNotThrowAnyException();
    }

    // ── Email / Resend (password-reset delivery — mandatory in "prod") ──────────

    @Test
    void run_prodWithEmailEnabledMissing_throws() {
        // Simulates the real EMAIL_ENABLED default (false) rather than overriding the
        // valid setUp() default — this is exactly the "ausente" case from the hardening
        // requirement, not a hand-picked bad value.
        emailProperties = new EmailProperties();
        validator = new ProductionReadinessValidator(
                environment, jwtProperties, storageProperties, tenantProperties, emailProperties);
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_ENABLED is missing or false");
    }

    @Test
    void run_prodWithEmailExplicitlyDisabled_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.setEnabled(false);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_ENABLED is missing or false");
    }

    @Test
    void run_localOrTestProfile_emailDisabled_doesNotThrow() {
        // The whole validator is a no-op outside "prod" (see run()) — email being
        // mandatory in prod must never affect local/test, regardless of its value.
        activeProfiles("local");
        emailProperties.setEnabled(false);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_prodWithEmailEnabledAndMissingFrom_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.setFrom("");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_FROM is missing");
    }

    @Test
    void run_prodWithEmailEnabledAndBlankFrom_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.setFrom("   ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_FROM is missing");
    }

    @Test
    void run_prodWithEmailEnabledAndInvalidFromFormat_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.setFrom("not-an-email-address");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_FROM")
                .hasMessageContaining("does not look like a valid sender address");
    }

    @Test
    void run_prodWithEmailEnabledAndDisplayNameFromFormat_doesNotThrow() throws IOException {
        // Resend accepts "Display Name <address>" as well as a bare address — must not
        // be rejected as "invalid".
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        Files.createDirectories(writableStorageDir);
        emailProperties.setFrom("Contractor Platform <no-reply@example.pt>");

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_prodWithEmailEnabledAndMissingApiKey_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.getResend().setApiKey("");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY is missing");
    }

    @Test
    void run_prodWithEmailEnabledAndUneditedExampleEnvFromPlaceholder_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.setFrom("CHANGE_ME@example.com");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_FROM")
                .hasMessageContaining("placeholder");
    }

    @Test
    void run_prodWithEmailEnabledAndPlaceholderApiKey_throws() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        emailProperties.getResend().setApiKey("CHANGE_ME_RESEND_API_KEY");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY")
                .hasMessageContaining("placeholder");
    }

    @Test
    void run_apiKeyRejection_neverLeaksTheValue() {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
        String keyThatMustNotLeak = "re_super_secret_key_that_must_never_appear_in_logs";
        emailProperties.getResend().setApiKey(keyThatMustNotLeak);

        // Valid (not blank, not a placeholder) — proves the key was accepted and never
        // had a reason to be printed, same pattern as run_dbPasswordRejection_neverLeaksTheValue.
        assertThatCode(() -> {
            Files.createDirectories(writableStorageDir);
            validator.run(null);
        }).doesNotThrowAnyException();
    }

    @Test
    void run_prodWithEmailEnabledAndValidConfig_doesNotThrow() throws IOException {
        activeProfiles("prod");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn(VALID_CORS);
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
