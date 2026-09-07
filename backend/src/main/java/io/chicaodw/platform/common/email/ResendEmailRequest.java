package io.chicaodw.platform.common.email;

import java.util.List;

/**
 * Request body for {@code POST https://api.resend.com/emails} — only the fields this
 * project actually uses (Resend's API accepts several more: cc/bcc/attachments/tags/
 * scheduled_at/template/etc., all irrelevant to a single transactional password-reset
 * email). See <a href="https://resend.com/docs/api-reference/emails/send-email">Resend
 * API reference</a>.
 */
record ResendEmailRequest(String from, List<String> to, String subject, String html, String text) {}
