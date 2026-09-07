package io.chicaodw.platform.common.config;

import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.common.email.EmailProperties;
import io.chicaodw.platform.common.storage.StorageProperties;
import io.chicaodw.platform.company.infrastructure.config.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fails application startup with a clear, secret-free message when the "prod" profile
 * is active and a required piece of configuration is missing, a known placeholder, or
 * otherwise unsafe to run with. A no-op in every other profile (default, local, test)
 * — {@code ./mvnw test} and local development are never affected by these checks.
 *
 * See docs/design/DT-011A.2-production-configuration.md for the full rationale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionReadinessValidator implements ApplicationRunner {

    private static final String PROD_PROFILE = "prod";
    private static final int MIN_JWT_SECRET_BYTES = 32;

    /** Known-bad JWT secrets that must never reach production — the dev fallback in
     * application.yml and the placeholder shipped in infra/env/production.env.example. */
    private static final Set<String> KNOWN_JWT_SECRET_PLACEHOLDERS = Set.of(
            "dev-only-secret-at-least-32-chars-change-in-prod!",
            "CHANGE_ME_WITH_A_STRONG_SECRET_MIN_32_CHARS"
    );

    // Sprint 12.4.2 (RR-04/RR-05) — a single, distinctive marker every unedited
    // placeholder in infra/env/production.env.example contains verbatim (JWT_SECRET's
    // own placeholder above included). Deliberately NOT a loose word like "change" —
    // that would reject legitimate values that merely contain a common substring
    // (e.g. a company called "Exchange Services"), which is exactly what was asked not
    // to do. "CHANGE_ME" as a literal, upper-snake-case token essentially never occurs
    // by coincidence in a real configuration value.
    private static final String PLACEHOLDER_MARKER = "CHANGE_ME";

    // RFC 2606 reserves example.com/.org/.net specifically for documentation — exactly
    // what infra/env/production.env.example's own placeholders use. Matched by exact
    // host or dot-suffix (never a bare substring), so "example.com" is rejected but
    // "myexample.com"/"example-store.com" are not.
    private static final Set<String> RESERVED_EXAMPLE_HOSTS = Set.of("example.com", "example.org", "example.net");

    private static final String DEV_DB_PASSWORD_DEFAULT = "platform";

    // Deliberately not a full RFC 5322 parser — same "light, good-enough" philosophy as
    // validateAbsoluteHttpUrl above (java.net.URI, no dedicated validation library).
    // Accepts a bare address ("no-reply@domain.tld") or Resend's "Display Name
    // <address@domain.tld>" form; only guards against an EMAIL_FROM that plainly isn't
    // an email address at all (e.g. a copy-paste mistake), not exotic-but-legal syntax.
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final StorageProperties storageProperties;
    private final TenantProperties tenantProperties;
    private final EmailProperties emailProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfileActive()) {
            return;
        }

        validateJwtSecret();
        validateJwtIssuerAudience();
        validateCorsOrigins();
        validatePlatformBaseDomain();
        validatePlatformFrontendBaseUrl();
        validateDatabasePassword();
        validateEmailConfiguration();
        validateStoragePath();

        log.info("Production readiness checks passed (profile '{}').", PROD_PROFILE);
    }

    // ── Shared placeholder/localhost detection ──────────────────────────────────

    private void rejectIfPlaceholder(String value, String varName) {
        if (value.contains(PLACEHOLDER_MARKER)) {
            throw new IllegalStateException(
                    varName + " is still set to a placeholder value (contains \"" + PLACEHOLDER_MARKER
                            + "\"). Replace it with a real value before running with profile '" + PROD_PROFILE + "'.");
        }
    }

    private boolean isReservedExampleHost(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        return RESERVED_EXAMPLE_HOSTS.stream().anyMatch(reserved -> lower.equals(reserved) || lower.endsWith("." + reserved));
    }

    private boolean isLocalhostHost(String host) {
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1");
    }

    /** Parses {@code value} as an absolute http(s) URL and rejects it if the scheme is
     * wrong, the host is missing, localhost/loopback, or an RFC 2606 reserved example
     * host — the same three checks {@link #validateOrigin} already applies to each CORS
     * origin, factored out so {@link #validatePlatformFrontendBaseUrl} can reuse them
     * without duplicating the URI-parsing logic. */
    private void validateAbsoluteHttpUrl(String value, String varName) {
        rejectIfPlaceholder(value, varName);

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(varName + " (\"" + value + "\") is not a valid URL.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalStateException(varName + " (\"" + value + "\") must use the http or https scheme.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(varName + " (\"" + value + "\") is not a valid absolute URL.");
        }

        if (isLocalhostHost(host)) {
            throw new IllegalStateException(
                    varName + " (\"" + value + "\") is a localhost origin, not allowed when profile '"
                            + PROD_PROFILE + "' is active.");
        }

        if (isReservedExampleHost(host)) {
            throw new IllegalStateException(
                    varName + " (\"" + value + "\") is still set to the documentation-only example.com/.org/.net "
                            + "domain from infra/env/production.env.example. Replace it with the real value.");
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
    }

    // ── JWT ──────────────────────────────────────────────────────────────────

    private void validateJwtSecret() {
        String secret = jwtProperties.getSecret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing. Set a strong, unique secret (see infra/env/production.env.example).");
        }
        if (KNOWN_JWT_SECRET_PLACEHOLDERS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still set to a known placeholder value. Generate a real secret before running "
                            + "with profile '" + PROD_PROFILE + "'.");
        }

        int byteLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + byteLength + " bytes; HS256 requires at least "
                            + MIN_JWT_SECRET_BYTES + "). Generate a stronger secret.");
        }
    }

    private void validateJwtIssuerAudience() {
        if (jwtProperties.getIssuer() == null || jwtProperties.getIssuer().isBlank()) {
            throw new IllegalStateException(
                    "JWT_ISSUER is missing. Every issued token must carry an issuer, and it is required on "
                            + "every parse — see docs/design/DT-011B.2 for why.");
        }
        if (jwtProperties.getAudience() == null || jwtProperties.getAudience().isBlank()) {
            throw new IllegalStateException(
                    "JWT_AUDIENCE is missing. Every issued token must carry an audience, and it is required on "
                            + "every parse — see docs/design/DT-011B.2 for why.");
        }
    }

    // ── CORS ─────────────────────────────────────────────────────────────────

    private void validateCorsOrigins() {
        String raw = environment.getProperty("app.cors.allowed-origins");
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS is required when profile '" + PROD_PROFILE + "' is active.");
        }

        // limit -1 preserves blank entries from a stray/trailing/leading comma instead
        // of silently dropping them, so "https://a.pt,,https://b.pt" is caught below.
        List<String> rawOrigins = Arrays.asList(raw.split(",", -1));
        for (String rawOrigin : rawOrigins) {
            validateOrigin(rawOrigin.trim());
        }
    }

    private void validateOrigin(String origin) {
        if (origin.isBlank()) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS contains a blank entry — check for stray commas.");
        }
        if (origin.equals("*")) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS must not contain a wildcard ('*') in production.");
        }

        validateAbsoluteHttpUrl(origin, "APP_CORS_ALLOWED_ORIGINS entry");
    }

    // ── Multi-tenant platform config (Sprint 12.4.2, RR-04/RR-05) ──────────────

    private void validatePlatformBaseDomain() {
        String domain = tenantProperties.getBaseDomain();
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException(
                    "PLATFORM_BASE_DOMAIN is missing. Required when profile '" + PROD_PROFILE + "' is active.");
        }

        rejectIfPlaceholder(domain, "PLATFORM_BASE_DOMAIN");

        if (isLocalhostHost(domain)) {
            throw new IllegalStateException(
                    "PLATFORM_BASE_DOMAIN (\"" + domain + "\") is a localhost value, not allowed when profile '"
                            + PROD_PROFILE + "' is active — tenant subdomain resolution needs a real domain.");
        }

        if (isReservedExampleHost(domain)) {
            throw new IllegalStateException(
                    "PLATFORM_BASE_DOMAIN (\"" + domain + "\") is still set to the documentation-only "
                            + "example.com/.org/.net domain from infra/env/production.env.example. Replace it with "
                            + "the real value.");
        }
    }

    private void validatePlatformFrontendBaseUrl() {
        String url = tenantProperties.getFrontendBaseUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "PLATFORM_FRONTEND_BASE_URL is missing. Required when profile '" + PROD_PROFILE
                            + "' is active — password-reset links are built from it (DT-011A.10).");
        }

        validateAbsoluteHttpUrl(url, "PLATFORM_FRONTEND_BASE_URL");
    }

    // ── Database ─────────────────────────────────────────────────────────────

    private void validateDatabasePassword() {
        String password = environment.getProperty("spring.datasource.password");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "DB_PASSWORD is missing. Required when profile '" + PROD_PROFILE + "' is active.");
        }

        if (password.equals(DEV_DB_PASSWORD_DEFAULT)) {
            throw new IllegalStateException(
                    "DB_PASSWORD is still set to the local development default. Generate a real password before "
                            + "running with profile '" + PROD_PROFILE + "'.");
        }

        rejectIfPlaceholder(password, "DB_PASSWORD");
    }

    // ── Email (password-reset delivery via Resend) ──────────────────────────────

    /**
     * Mandatory in "prod" — hardened after go-live review: self-service password
     * recovery has no other delivery channel for a regular OWNER today (the
     * admin-assisted link, DT-011A.10 §3, is not a substitute at the current product
     * stage), so a production boot with email delivery silently off is a functional
     * regression, not a valid deployment state. {@code app.email.enabled} still
     * defaults to {@code false} — that default is what keeps local/test environments
     * unaffected (this whole check is skipped outside "prod", see {@link #run}) — but
     * "prod" now requires it explicitly set to {@code true}, plus a real
     * {@code EMAIL_FROM}/{@code RESEND_API_KEY}, exactly like every other required
     * secret this validator already enforces (JWT_SECRET, DB_PASSWORD, etc.).
     */
    private void validateEmailConfiguration() {
        if (!emailProperties.isEnabled()) {
            throw new IllegalStateException(
                    "EMAIL_ENABLED is missing or false. Password recovery has no delivery channel for a regular "
                            + "OWNER without it — required when profile '" + PROD_PROFILE + "' is active.");
        }

        String from = emailProperties.getFrom();
        if (from == null || from.isBlank()) {
            throw new IllegalStateException(
                    "EMAIL_FROM is missing. Required when profile '" + PROD_PROFILE
                            + "' is active and EMAIL_ENABLED=true.");
        }
        rejectIfPlaceholder(from, "EMAIL_FROM");
        if (!SIMPLE_EMAIL_PATTERN.matcher(extractEmailAddress(from)).matches()) {
            throw new IllegalStateException(
                    "EMAIL_FROM (\"" + from + "\") does not look like a valid sender address. Expected "
                            + "\"user@domain.tld\" or \"Display Name <user@domain.tld>\".");
        }

        String apiKey = emailProperties.getResend().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "RESEND_API_KEY is missing. Required when profile '" + PROD_PROFILE
                            + "' is active and EMAIL_ENABLED=true.");
        }
        rejectIfPlaceholder(apiKey, "RESEND_API_KEY");
    }

    /** Resend accepts either a bare address or {@code "Display Name <address>"} — pulls
     * out just the address part so the format check below applies to the same string
     * either way. EMAIL_FROM is not a secret (it's shown to every email recipient), so
     * unlike the API key it's safe to echo back in an error message. */
    private String extractEmailAddress(String from) {
        int start = from.indexOf('<');
        int end = from.indexOf('>');
        if (start >= 0 && end > start) {
            return from.substring(start + 1, end).trim();
        }
        return from.trim();
    }

    // ── Storage ──────────────────────────────────────────────────────────────

    private void validateStoragePath() {
        Path baseDir = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize();

        if (!Files.exists(baseDir)) {
            try {
                Files.createDirectories(baseDir);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "STORAGE_PATH (" + baseDir + ") does not exist and could not be created.", e);
            }
        }

        if (!Files.isDirectory(baseDir)) {
            throw new IllegalStateException("STORAGE_PATH (" + baseDir + ") exists but is not a directory.");
        }

        if (!Files.isWritable(baseDir)) {
            throw new IllegalStateException(
                    "STORAGE_PATH (" + baseDir + ") is not writable by the application user.");
        }
    }
}
