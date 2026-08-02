package io.chicaodw.platform.common.config;

import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.common.storage.StorageProperties;
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
import java.util.Set;

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

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final StorageProperties storageProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfileActive()) {
            return;
        }

        validateJwtSecret();
        validateJwtIssuerAudience();
        validateCorsOrigins();
        validateStoragePath();

        log.info("Production readiness checks passed (profile '{}').", PROD_PROFILE);
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

        URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS entry \"" + origin + "\" is not a valid URL.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS entry \"" + origin + "\" must use the http or https scheme.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS entry \"" + origin + "\" is not a valid absolute URL.");
        }

        if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS entry \"" + origin + "\" is a localhost origin, not allowed when "
                            + "profile '" + PROD_PROFILE + "' is active.");
        }
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
