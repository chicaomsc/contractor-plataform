package io.chicaodw.platform.common.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Calls the Resend REST API directly ({@code POST https://api.resend.com/emails}) via
 * the JDK's own {@link HttpClient} — deliberately not the {@code resend-java} SDK.
 * Resend's send-email call is one POST request with a small JSON body and a bearer
 * token; the SDK would add a dependency (with its own version to track and audit) for
 * something {@link HttpClient} + the {@link ObjectMapper} already on the classpath do
 * completely. This keeps the Resend integration self-contained in this one class, with
 * zero new third-party dependency (see {@code pom.xml} — unchanged by this feature).
 *
 * <p>Never throws — see {@link EmailService}'s class Javadoc for why. Never logs the
 * reset token, the reset link, or the API key; see the one {@code log.warn} below for
 * exactly what does get logged on failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResendEmailService implements EmailService {

    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final EmailProperties properties;

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetLink, Duration validity) {
        if (!properties.isEnabled()) {
            log.info("Password-reset email not sent: app.email.enabled=false");
            return;
        }

        try {
            HttpResponse<String> response = httpClient.send(buildRequest(recipientEmail, resetLink, validity),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("Password-reset email rejected by Resend (provider=resend, status={})", response.statusCode());
                return;
            }

            log.info("Password-reset email sent (provider=resend, status={})", response.statusCode());
        } catch (InterruptedException e) {
            // httpClient.send(...) is the blocking overload, so it can throw this when the
            // calling thread is interrupted mid-request (e.g. Tomcat worker shutdown). The
            // interrupt flag must be restored — swallowing it silently would hide a real
            // cancellation/shutdown signal from the rest of the call stack — but the failure
            // itself still must not propagate: same never-throw contract as every other
            // failure mode here. See EmailService's class Javadoc.
            Thread.currentThread().interrupt();
            log.warn("Password-reset email interrupted (provider=resend, error=InterruptedException)");
        } catch (IOException e) {
            // Network failures and HttpTimeoutException (an IOException subtype, thrown on
            // REQUEST_TIMEOUT above) both land here, plus JSON serialization failures from
            // buildRequest. Whatever the cause, the caller (PasswordResetTokenService) must
            // see nothing but a normal return, and the public HTTP response it builds must
            // stay exactly as uniform as if this method had succeeded. See EmailService's
            // class Javadoc. e.getClass().getSimpleName() plus the exception itself (for the
            // stack trace) is all that's logged — never the request body, so never the reset
            // token/link or the API key.
            log.warn("Failed to send password-reset email (provider=resend, error={})", e.getClass().getSimpleName(), e);
        } catch (RuntimeException e) {
            // Defense in depth against anything unexpected (e.g. a bug in request
            // construction) — same reasoning and same never-throw guarantee as above.
            log.warn("Failed to send password-reset email (provider=resend, error={})", e.getClass().getSimpleName(), e);
        }
    }

    private HttpRequest buildRequest(String recipientEmail, String resetLink, Duration validity)
            throws JsonProcessingException {
        ResendEmailRequest body = new ResendEmailRequest(
                properties.getFrom(),
                List.of(recipientEmail),
                PasswordResetEmailTemplate.SUBJECT,
                PasswordResetEmailTemplate.html(resetLink, validity),
                PasswordResetEmailTemplate.text(resetLink, validity));

        return HttpRequest.newBuilder(RESEND_ENDPOINT)
                .header("Authorization", "Bearer " + properties.getResend().getApiKey())
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }
}
