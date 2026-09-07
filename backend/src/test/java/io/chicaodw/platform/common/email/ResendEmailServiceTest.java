package io.chicaodw.platform.common.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit test — {@link HttpClient} is mocked, so this never makes a real network
 * call to Resend (or anywhere else). See {@code PasswordResetEmailFlowTest} for the
 * integration-level test that exercises {@code PasswordResetTokenService} calling this
 * interface (with {@code EmailService} itself mocked there instead).
 *
 * <p>{@code doReturn}/{@code doThrow} (not {@code when(...).thenReturn(...)}) throughout:
 * {@link HttpClient#send} is generic ({@code <T> HttpResponse<T> send(HttpRequest,
 * BodyHandler<T>)}), and {@code any()} for the {@code BodyHandler} argument makes the
 * compiler infer {@code HttpResponse<Object>} for {@code when(...)}, which then rejects
 * a {@code HttpResponse<String>} mock — {@code doReturn} sidesteps this by accepting a
 * plain {@code Object}, the standard workaround for mocking this exact JDK method.
 */
@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

    private static final String RECIPIENT = "owner@example.pt";
    private static final String RESET_LINK = "https://app.example.pt/reset-password#token=abc123";
    private static final Duration VALIDITY = Duration.ofMinutes(30);

    @Mock
    HttpClient httpClient;

    @Mock
    HttpResponse<String> httpResponse;

    EmailProperties properties;
    ResendEmailService service;

    @BeforeEach
    void setUp() {
        properties = new EmailProperties();
        properties.setEnabled(true);
        properties.setFrom("Contractor Platform <no-reply@example.pt>");
        properties.getResend().setApiKey("re_test_key_not_real");

        service = new ResendEmailService(httpClient, new ObjectMapper(), properties);
    }

    @Test
    void disabled_neverCallsHttpClient() throws Exception {
        properties.setEnabled(false);

        service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY);

        verifyNoInteractions(httpClient);
    }

    @Test
    void enabled_success_callsHttpClientOnceWithExpectedRequest() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        assertThatCode(() -> service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY))
                .doesNotThrowAnyException();

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(captor.capture(), any());

        HttpRequest sent = captor.getValue();
        assertThat(sent.headers().firstValue("Authorization")).contains("Bearer re_test_key_not_real");
        assertThat(sent.uri().toString()).isEqualTo("https://api.resend.com/emails");
        assertThat(sent.method()).isEqualTo("POST");
    }

    @Test
    void requestBody_containsRecipientResetLinkAndSender() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY);
        verify(httpClient).send(captor.capture(), any());

        Map<?, ?> json = new ObjectMapper().readValue(bodyAsString(captor.getValue()), Map.class);

        assertThat(json.get("to")).isEqualTo(List.of(RECIPIENT));
        assertThat((String) json.get("html")).contains(RESET_LINK);
        assertThat((String) json.get("text")).contains(RESET_LINK);
        assertThat(json.get("from")).isEqualTo(properties.getFrom());
        assertThat(json.get("subject")).isEqualTo("Recupere sua senha");
    }

    @Test
    void providerReturns4xx_doesNotThrow() throws Exception {
        when(httpResponse.statusCode()).thenReturn(422);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        assertThatCode(() -> service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY))
                .doesNotThrowAnyException();
    }

    @Test
    void providerReturns5xx_doesNotThrow() throws Exception {
        when(httpResponse.statusCode()).thenReturn(503);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        assertThatCode(() -> service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY))
                .doesNotThrowAnyException();
    }

    @Test
    void providerTimeout_doesNotThrow() throws Exception {
        doThrow(new HttpTimeoutException("timeout")).when(httpClient).send(any(), any());

        assertThatCode(() -> service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY))
                .doesNotThrowAnyException();
    }

    @Test
    void providerNetworkError_doesNotThrow() throws Exception {
        doThrow(new IOException("connection refused")).when(httpClient).send(any(), any());

        assertThatCode(() -> service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY))
                .doesNotThrowAnyException();
    }

    @Test
    void providerInterrupted_doesNotThrow() throws Exception {
        doThrow(new InterruptedException("interrupted")).when(httpClient).send(any(), any());

        assertThatCode(() -> service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY))
                .doesNotThrowAnyException();
    }

    @Test
    void providerInterrupted_restoresInterruptFlag() throws Exception {
        doThrow(new InterruptedException("interrupted")).when(httpClient).send(any(), any());

        try {
            service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY);

            // Thread.interrupted() both reads AND clears the flag — must check first,
            // then explicitly clear in the finally block below regardless, so a failed
            // assertion here can't leave this JUnit worker thread interrupted for
            // whatever test runs on it next.
            assertThat(Thread.interrupted())
                    .as("interrupt flag must be restored, not silently swallowed")
                    .isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void neverCalledMoreThanOncePerRequest() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.sendPasswordResetEmail(RECIPIENT, RESET_LINK, VALIDITY);

        verify(httpClient, times(1)).send(any(), any());
    }

    /** Drains the request's {@code BodyPublisher} synchronously so the test can assert on it. */
    private static String bodyAsString(HttpRequest request) {
        var publisher = request.bodyPublisher().orElseThrow();
        var result = new AtomicReference<String>();
        publisher.subscribe(new Flow.Subscriber<>() {
            private final StringBuilder sb = new StringBuilder();

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                sb.append(new String(bytes, StandardCharsets.UTF_8));
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                result.set(sb.toString());
            }
        });
        return result.get();
    }
}
