package io.chicaodw.platform.common.email;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetEmailTemplateTest {

    private static final String RESET_LINK = "https://app.example.pt/reset-password#token=abc123XYZ_-";

    @Test
    void text_containsExactResetLink() {
        String text = PasswordResetEmailTemplate.text(RESET_LINK, Duration.ofMinutes(30));

        assertThat(text).contains(RESET_LINK);
    }

    @Test
    void html_containsExactResetLinkAsHref() {
        String html = PasswordResetEmailTemplate.html(RESET_LINK, Duration.ofMinutes(30));

        assertThat(html).contains("href=\"" + RESET_LINK + "\"");
    }

    @Test
    void text_mentionsValidityInMinutes() {
        String text = PasswordResetEmailTemplate.text(RESET_LINK, Duration.ofMinutes(45));

        assertThat(text).contains("45 minutos");
    }

    @Test
    void html_mentionsValidityInMinutes() {
        String html = PasswordResetEmailTemplate.html(RESET_LINK, Duration.ofMinutes(45));

        assertThat(html).contains("45 minutos");
    }

    @Test
    void text_mentionsIgnoreIfNotRequested() {
        String text = PasswordResetEmailTemplate.text(RESET_LINK, Duration.ofMinutes(30));

        assertThat(text).containsIgnoringCase("ignorar");
    }

    @Test
    void neitherVersionContainsAnythingThatLooksLikeARawPassword() {
        String text = PasswordResetEmailTemplate.text(RESET_LINK, Duration.ofMinutes(30));
        String html = PasswordResetEmailTemplate.html(RESET_LINK, Duration.ofMinutes(30));

        assertThat(text.toLowerCase()).doesNotContain("senha:").doesNotContain("password:");
        assertThat(html.toLowerCase()).doesNotContain("senha:").doesNotContain("password:");
    }

    @Test
    void subject_isTheExpectedFixedString() {
        assertThat(PasswordResetEmailTemplate.SUBJECT).isEqualTo("Recupere sua senha");
    }
}
