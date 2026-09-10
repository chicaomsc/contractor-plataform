package io.chicaodw.platform.common.email;

import java.time.Duration;

/**
 * Same reasoning as {@link PasswordResetEmailTemplate}: plain string templates, no
 * templating engine — one transactional email, static copy, a handful of variable
 * slots. Deliberately never mentions {@code SUPER_ADMIN}/{@code OWNER} or any other
 * internal role name — {@code friendlyRoleName} is the only role-shaped text a
 * recipient sees (DT-017B §10).
 */
final class TeamInvitationEmailTemplate {

    static final String SUBJECT = "Convite para equipe";

    private TeamInvitationEmailTemplate() {
    }

    static String text(String companyName, String friendlyRoleName, String acceptLink, Duration validity) {
        return """
                Você foi convidado para participar da equipe de %s como %s.

                Para aceitar o convite e criar sua conta, acesse o link abaixo:
                %s

                Este link é válido por aproximadamente %d dias e só pode ser usado uma vez.

                Se você não esperava este convite, pode ignorar este e-mail com segurança.
                """.formatted(companyName, friendlyRoleName, acceptLink, validity.toDays());
    }

    static String html(String companyName, String friendlyRoleName, String acceptLink, Duration validity) {
        // companyName is OWNER-controlled free text (Company.name) — unlike
        // friendlyRoleName (always one of this class's own two literals) it must be
        // escaped before landing in an HTML body; the plain-text template above needs
        // no such escaping.
        String safeCompanyName = escapeHtml(companyName);
        return """
                <!DOCTYPE html>
                <html lang="pt">
                  <body style="font-family: Arial, Helvetica, sans-serif; color: #1a1a1a; line-height: 1.5; max-width: 480px; margin: 0 auto;">
                    <p>Você foi convidado para participar da equipe de <strong>%s</strong> como <strong>%s</strong>.</p>
                    <p>
                      <a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #1a56db; color: #ffffff; text-decoration: none; border-radius: 6px;">
                        Aceitar convite
                      </a>
                    </p>
                    <p>Ou copie e cole este link no seu navegador:<br>
                      <a href="%s">%s</a>
                    </p>
                    <p>Este link é válido por aproximadamente %d dias e só pode ser usado uma vez.</p>
                    <p style="color: #666666; font-size: 13px;">
                      Se você não esperava este convite, pode ignorar este e-mail com segurança.
                    </p>
                  </body>
                </html>
                """.formatted(safeCompanyName, friendlyRoleName, acceptLink, acceptLink, acceptLink, validity.toDays());
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
