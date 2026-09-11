package io.chicaodw.platform.common.email;

import java.time.Duration;

/**
 * Port for transactional email delivery. Deliberately narrow (one purpose-built method,
 * not a generic {@code send(subject, body)}) so the caller never has to build message
 * content itself — same shape as {@link io.chicaodw.platform.common.storage.StorageService}:
 * the domain/application layer depends only on this interface, never on a provider SDK
 * or HTTP client (see {@code ResendEmailService} for the only implementation today).
 *
 * <p><b>Contract: implementations must never throw.</b> A provider outage, timeout, or
 * error response must never propagate to the caller — {@code PasswordResetTokenService}
 * relies on this to keep the public {@code POST /auth/password/forgot} response uniform
 * regardless of email delivery outcome (never reveals whether the account exists, or
 * whether the email was sent). Implementations must catch their own failures and log
 * them; the caller intentionally does not (and must not have to) handle a checked or
 * unchecked exception from this call.
 */
public interface EmailService {

    /**
     * Sends the password-reset email. A no-op (returns normally, does nothing) when
     * email sending is disabled ({@code app.email.enabled=false}) — this is the default
     * outside a deliberately configured environment, so local/test runs never make a
     * real network call.
     *
     * @param recipientEmail the account's own email address (already resolved/validated
     *                        by the caller — this method does no eligibility checking)
     * @param resetLink       the exact link built by {@code PasswordResetTokenService.buildResetLink}
     *                        — never reconstructed here
     * @param validity        how long the link remains usable, for display in the email body only
     */
    void sendPasswordResetEmail(String recipientEmail, String resetLink, Duration validity);

    /**
     * Sends the team-invitation email (DT-017B). Same never-throw contract as {@link
     * #sendPasswordResetEmail} — {@code TeamInvitationService} relies on it the same
     * way {@code PasswordResetTokenService} does: a Resend outage must never turn
     * invitation creation into a different HTTP response, and must never leave the
     * caller having to handle an exception from this call.
     *
     * @param recipientEmail   the invited person's email — exactly {@code
     *                          TeamInvitation.email}, never anything client-supplied
     * @param companyName      the inviting Company's display name, shown as free text in
     *                          the email body (implementations must treat it as untrusted
     *                          — see {@code TeamInvitationEmailTemplate})
     * @param friendlyRoleName a human-facing label for the invited role (e.g.
     *                          "Administrador"/"Colaborador") — never the technical enum
     *                          name, and never SUPER_ADMIN/OWNER (DT-017B §10)
     * @param acceptLink       the exact link built by {@code TeamInvitationService} —
     *                          never reconstructed here
     * @param validity         how long the link remains usable, for display only
     */
    void sendTeamInvitationEmail(String recipientEmail, String companyName, String friendlyRoleName,
            String acceptLink, Duration validity);
}
