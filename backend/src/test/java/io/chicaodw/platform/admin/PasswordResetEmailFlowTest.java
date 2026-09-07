package io.chicaodw.platform.admin;

import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordRequest;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordResponse;
import io.chicaodw.platform.common.email.EmailService;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Email dispatch on top of the password-reset flow (see {@link PasswordResetFlowTest}
 * for the mechanism itself, unchanged by this feature). {@code app.email.enabled=true}
 * here (its own Spring context, same pattern as {@code RateLimitTest}), with
 * {@link EmailService} mocked — never a real Resend call; see {@code ResendEmailServiceTest}
 * for the provider-facing behavior (success/failure/timeout) tested in isolation.
 */
@SpringBootTest(properties = {
        "app.email.enabled=true",
        "app.email.from=no-reply@example.test",
        // Explicit, not assumed-inherited from AbstractIntegrationTest's own
        // @SpringBootTest(properties=...) — this class's distinct property set already
        // creates its own cached context, and several tests here call
        // /auth/register + /auth/password/forgot enough times in one run to trip the
        // real capacities (register=5/hour, forgot-password=5/60s) if left enabled.
        "app.rate-limit.enabled=false",
        "app.security.bcrypt-strength=4",
})
class PasswordResetEmailFlowTest extends AbstractAdminIntegrationTest {

    private static final String GENERIC_FORGOT_MESSAGE = "Se existir uma conta para este email, as instruções foram geradas.";

    @Autowired CompanyRepository companyRepository;

    @MockitoBean
    EmailService emailService;

    @Test
    void forgot_eligibleUser_callsEmailServiceExactlyOnce_withCorrectRecipientAndResetLink() throws Exception {
        var owner = registerOwner();

        ForgotPasswordResponse response = forgot(owner.email());

        verify(emailService, times(1)).sendPasswordResetEmail(eq(owner.email()), anyString(), any(Duration.class));
        // The debug link is only exposed outside "prod" — reuse it here just to assert
        // the email's link is the exact same one the response/DB already agree on,
        // never a second, independently-built URL/token.
        verify(emailService).sendPasswordResetEmail(anyString(), eq(response.debugResetLink()), any());
        verify(emailService).sendPasswordResetEmail(anyString(), contains("/reset-password#token="), any());
    }

    @Test
    void forgot_unknownEmail_neverCallsEmailService() throws Exception {
        forgot("does-not-exist-" + System.nanoTime() + "@example.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void forgot_ineligibleAccount_neverCallsEmailService() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var seedOwner = registerOwner();
        UUID companyId = companyRepository.findBySlug(seedOwner.companySlug()).orElseThrow().getId();
        String pendingEmail = "pending-email-test-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/admin/companies/" + companyId + "/owners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteOwnerRequest("Pending", pendingEmail))))
                .andExpect(status().isCreated());

        forgot(pendingEmail);

        verifyNoInteractions(emailService);
    }

    @Test
    void forgot_withinCooldown_secondRequestNeverCallsEmailServiceAgain() throws Exception {
        var owner = registerOwner();

        forgot(owner.email());
        forgot(owner.email());

        verify(emailService, times(1)).sendPasswordResetEmail(eq(owner.email()), anyString(), any());
    }

    @Test
    void forgot_emailServiceThrows_responseStaysGenericAnd200() throws Exception {
        var owner = registerOwner();
        doThrow(new RuntimeException("Resend is on fire"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString(), any());

        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(owner.email()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ForgotPasswordResponse forgot(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, ForgotPasswordResponse.class);
    }
}
