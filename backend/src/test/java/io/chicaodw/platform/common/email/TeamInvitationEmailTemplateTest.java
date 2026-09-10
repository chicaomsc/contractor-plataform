package io.chicaodw.platform.common.email;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TeamInvitationEmailTemplateTest {

    private static final String ACCEPT_LINK = "https://app.example.pt/invite/team#token=abc123XYZ_-";
    private static final String COMPANY_NAME = "Acme Contractors";

    @Test
    void text_containsExactAcceptLink() {
        String text = TeamInvitationEmailTemplate.text(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(text).contains(ACCEPT_LINK);
    }

    @Test
    void html_containsExactAcceptLinkAsHref() {
        String html = TeamInvitationEmailTemplate.html(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(html).contains("href=\"" + ACCEPT_LINK + "\"");
    }

    @Test
    void text_mentionsCompanyNameAndFriendlyRole() {
        String text = TeamInvitationEmailTemplate.text(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(text).contains(COMPANY_NAME).contains("Administrador");
    }

    @Test
    void html_mentionsCompanyNameAndFriendlyRole() {
        String html = TeamInvitationEmailTemplate.html(COMPANY_NAME, "Colaborador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(html).contains(COMPANY_NAME).contains("Colaborador");
    }

    @Test
    void text_mentionsValidityInDays() {
        String text = TeamInvitationEmailTemplate.text(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(text).contains("7 dias");
    }

    @Test
    void html_mentionsValidityInDays() {
        String html = TeamInvitationEmailTemplate.html(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(html).contains("7 dias");
    }

    @Test
    void html_escapesCompanyNameContainingHtmlSpecialCharacters() {
        String html = TeamInvitationEmailTemplate.html(
                "<script>alert(1)</script> & Sons", "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; Sons");
    }

    @Test
    void neitherVersionMentionsInternalRoleNames() {
        String text = TeamInvitationEmailTemplate.text(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));
        String html = TeamInvitationEmailTemplate.html(COMPANY_NAME, "Administrador", ACCEPT_LINK, Duration.ofDays(7));

        assertThat(text).doesNotContain("SUPER_ADMIN").doesNotContain("OWNER").doesNotContain("MANAGER");
        assertThat(html).doesNotContain("SUPER_ADMIN").doesNotContain("OWNER").doesNotContain("MANAGER");
    }

    @Test
    void subject_isTheExpectedFixedString() {
        assertThat(TeamInvitationEmailTemplate.SUBJECT).isEqualTo("Convite para equipe");
    }
}
