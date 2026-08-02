package io.chicaodw.platform.auth.domain;

/**
 * The single source of truth for password length limits — referenced by every flow
 * that sets a password (register, invite acceptance, password reset, SUPER_ADMIN
 * bootstrap) so the rule can never silently drift between them (Sprint 11B.6D,
 * SEC-AUTH-09).
 *
 * Deliberately length-only, no composition rules (uppercase/digit/symbol): NIST
 * 800-63B recommends length over artificial complexity, which this project already
 * followed in practice before this class existed — it just wasn't declared in one
 * place. {@link #MAX_LENGTH} exists to bound bcrypt's input cost, not to discourage
 * long passphrases — 128 characters comfortably fits any realistic passphrase.
 *
 * The constants are compile-time constants on purpose (not {@code @ConfigurationProperties})
 * so they can be referenced directly from {@code @Size} annotations, which require a
 * constant expression — password length is a domain invariant, not deployment
 * configuration that should vary by environment.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;
    public static final String MESSAGE = "Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters";

    private PasswordPolicy() {
    }
}
