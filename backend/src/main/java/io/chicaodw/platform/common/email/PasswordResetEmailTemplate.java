package io.chicaodw.platform.common.email;

import java.time.Duration;

/**
 * Plain string templates — deliberately not a template engine (Thymeleaf/Freemarker/
 * etc.): one transactional email, static copy, a single variable slot (the link) and a
 * computed one (validity in minutes). Introducing a templating dependency for this
 * would be exactly the kind of disproportionate infrastructure this project avoids
 * elsewhere (see e.g. the storage/image-normalization Javadocs for the same reasoning
 * applied to other decisions).
 */
final class PasswordResetEmailTemplate {

    static final String SUBJECT = "Recupere sua senha";

    private PasswordResetEmailTemplate() {
    }

    static String text(String resetLink, Duration validity) {
        return """
                Recebemos uma solicitação para redefinir a senha da sua conta.

                Para continuar, acesse o link abaixo:
                %s

                Este link é válido por aproximadamente %d minutos e só pode ser usado uma vez.

                Se você não solicitou esta alteração, pode ignorar este e-mail com segurança \
                — sua senha atual continua funcionando normalmente.
                """.formatted(resetLink, validity.toMinutes());
    }

    static String html(String resetLink, Duration validity) {
        return """
                <!DOCTYPE html>
                <html lang="pt">
                  <body style="font-family: Arial, Helvetica, sans-serif; color: #1a1a1a; line-height: 1.5; max-width: 480px; margin: 0 auto;">
                    <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                    <p>
                      <a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #1a56db; color: #ffffff; text-decoration: none; border-radius: 6px;">
                        Redefinir senha
                      </a>
                    </p>
                    <p>Ou copie e cole este link no seu navegador:<br>
                      <a href="%s">%s</a>
                    </p>
                    <p>Este link é válido por aproximadamente %d minutos e só pode ser usado uma vez.</p>
                    <p style="color: #666666; font-size: 13px;">
                      Se você não solicitou esta alteração, pode ignorar este e-mail com segurança
                      — sua senha atual continua funcionando normalmente.
                    </p>
                  </body>
                </html>
                """.formatted(resetLink, resetLink, resetLink, validity.toMinutes());
    }
}
