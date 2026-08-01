package io.chicaodw.platform.admin;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.admin.api.dto.UpdateCompanyStatusRequest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordRequest;
import io.chicaodw.platform.auth.api.dto.ForgotPasswordResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.auth.api.dto.ResetPasswordRequest;
import io.chicaodw.platform.auth.domain.PasswordResetToken;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.PasswordResetTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;

/** Self-service password recovery — DT-011A.10 §2/§4/§6/§7/§9/§17. */
class PasswordResetFlowTest extends AbstractAdminIntegrationTest {

    private static final String GENERIC_FORGOT_MESSAGE = "Se existir uma conta para este email, as instruções foram geradas.";
    private static final String INVALID_LINK_MESSAGE = "O link de recuperação é inválido ou não está mais disponível.";

    @Autowired CompanyRepository companyRepository;
    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    // ── Forgot ───────────────────────────────────────────────────────────────

    @Test
    void forgot_existingActiveOwner_createsTokenAndReturnsDebugFields() throws Exception {
        var owner = registerOwner();

        ForgotPasswordResponse response = readForgotResponse(forgot(owner.email()));

        assertThat(response.message()).isEqualTo(GENERIC_FORGOT_MESSAGE);
        assertThat(response.debugToken()).isNotBlank();
        assertThat(response.debugResetLink()).contains("/reset-password#token=" + response.debugToken());
    }

    @Test
    void forgot_unknownEmail_returnsSameGenericResponse_noDebugFields() throws Exception {
        ForgotPasswordResponse response = readForgotResponse(forgot("does-not-exist-" + System.nanoTime() + "@example.com"));

        assertThat(response.message()).isEqualTo(GENERIC_FORGOT_MESSAGE);
        assertThat(response.debugToken()).isNull();
        assertThat(response.debugResetLink()).isNull();
    }

    @Test
    void forgot_isCaseInsensitive() throws Exception {
        var owner = registerOwner();

        ForgotPasswordResponse response = readForgotResponse(forgot(owner.email().toUpperCase()));

        assertThat(response.debugToken()).isNotBlank();
    }

    @Test
    void forgot_pendingOwner_returnsGenericResponse_noTokenCreated() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var seedOwner = registerOwner();
        UUID companyId = companyRepository.findBySlug(seedOwner.companySlug()).orElseThrow().getId();
        String pendingEmail = invitePendingOwner(adminToken, companyId);

        ForgotPasswordResponse response = readForgotResponse(forgot(pendingEmail));

        assertThat(response.debugToken()).isNull();
    }

    @Test
    void forgot_deactivatedCompanyOwner_returnsGenericResponse_noTokenCreated() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        setCompanyStatus(adminToken, companyId, "INACTIVE");

        ForgotPasswordResponse response = readForgotResponse(forgot(owner.email()));

        assertThat(response.debugToken()).isNull();
    }

    @Test
    void forgot_deactivatedSuperAdmin_returnsGenericResponse_noTokenCreated() throws Exception {
        String email = "deactivated-admin-" + System.nanoTime() + "@example.com";
        createSuperAdminAndLogin(email);
        User admin = userRepository.findByEmail(email).orElseThrow();
        admin.setStatus(UserStatus.INACTIVE);
        userRepository.save(admin);

        ForgotPasswordResponse response = readForgotResponse(forgot(email));

        assertThat(response.debugToken()).isNull();
    }

    @Test
    void forgot_newRequestOutsideCooldown_revokesPreviousToken() throws Exception {
        var owner = registerOwner();

        String firstToken = requestResetToken(owner.email());
        backdateTokenCreatedAt(firstToken, Instant.now().minusSeconds(600));

        String secondToken = requestResetToken(owner.email());

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(storedToken(firstToken).getRevokedAt()).isNotNull();
    }

    @Test
    void forgot_withinCooldown_doesNotCreateSecondToken_sameResponseShape() throws Exception {
        var owner = registerOwner();
        String firstToken = requestResetToken(owner.email());

        ForgotPasswordResponse secondResponse = readForgotResponse(forgot(owner.email()));

        assertThat(secondResponse.message()).isEqualTo(GENERIC_FORGOT_MESSAGE);
        assertThat(secondResponse.debugToken()).isNull();
        assertThat(storedToken(firstToken).getRevokedAt()).isNull();
    }

    @Test
    void forgot_malformedEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("not-an-email"))))
                .andExpect(status().isBadRequest());
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    @Test
    void reset_validToken_activatesNewPassword_allowsImmediateLogin_blocksOldPassword() throws Exception {
        var owner = registerOwner();
        String rawToken = requestResetToken(owner.email());

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "BrandNewPass1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha atualizada. Acesse sua conta novamente."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(owner.email(), "BrandNewPass1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(owner.email(), owner.password()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reset_unknownToken_returnsUniform422() throws Exception {
        attemptReset("does-not-exist", "BrandNewPass1")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(INVALID_LINK_MESSAGE));
    }

    @Test
    void reset_expiredToken_returnsUniform422() throws Exception {
        var owner = registerOwner();
        String rawToken = requestResetToken(owner.email());
        PasswordResetToken stored = storedToken(rawToken);
        stored.setExpiresAt(Instant.now().minusSeconds(60));
        passwordResetTokenRepository.save(stored);

        attemptReset(rawToken, "BrandNewPass1")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(INVALID_LINK_MESSAGE));
    }

    @Test
    void reset_alreadyUsedToken_secondAttemptReturnsUniform422() throws Exception {
        var owner = registerOwner();
        String rawToken = requestResetToken(owner.email());

        attemptReset(rawToken, "BrandNewPass1").andExpect(status().isOk());
        attemptReset(rawToken, "AnotherPass2")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(INVALID_LINK_MESSAGE));
    }

    @Test
    void reset_revokedToken_returnsUniform422() throws Exception {
        var owner = registerOwner();
        String firstToken = requestResetToken(owner.email());
        backdateTokenCreatedAt(firstToken, Instant.now().minusSeconds(600));
        requestResetToken(owner.email());

        attemptReset(firstToken, "BrandNewPass1")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(INVALID_LINK_MESSAGE));
    }

    @Test
    void reset_weakPassword_returnsBadRequest() throws Exception {
        var owner = registerOwner();
        String rawToken = requestResetToken(owner.email());

        attemptReset(rawToken, "short").andExpect(status().isBadRequest());
    }

    @Test
    void reset_samePasswordAsCurrent_returnsSpecific422() throws Exception {
        var owner = registerOwner();
        String rawToken = requestResetToken(owner.email());

        attemptReset(rawToken, owner.password())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("A nova password deve ser diferente da atual."));
    }

    @Test
    void reset_deactivatedCompanyOwner_returnsUniform422EvenWithValidToken() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        String rawToken = requestResetToken(owner.email());

        setCompanyStatus(adminToken, companyId, "INACTIVE");

        attemptReset(rawToken, "BrandNewPass1")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(INVALID_LINK_MESSAGE));
    }

    @Test
    void reset_revokesAllRefreshTokensAndBumpsAuthVersion_oldAccessTokenRejectedImmediately() throws Exception {
        var owner = registerOwner();
        String firstAccessToken = owner.accessToken();

        String secondLoginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(owner.email(), owner.password()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AuthResponse secondLogin = objectMapper.readValue(secondLoginBody, AuthResponse.class);

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk());

        String rawToken = requestResetToken(owner.email());
        attemptReset(rawToken, "BrandNewPass1").andExpect(status().isOk());

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Disabled"));

        User user = userRepository.findByEmail(owner.email()).orElseThrow();
        List<RefreshToken> tokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(user.getId()))
                .toList();
        assertThat(tokens).hasSizeGreaterThanOrEqualTo(2);
        assertThat(tokens).allMatch(RefreshToken::isRevoked);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(secondLogin.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reset_concurrentCompletions_onlyOneSucceeds() throws Exception {
        var owner = registerOwner();
        String rawToken = requestResetToken(owner.email());
        UUID tokenId = storedToken(rawToken).getId();

        // Each worker thread needs its own transaction — the repository's default
        // transactional wrapping is bound to the thread that opens it, and a bare
        // ExecutorService callable calling the repository directly runs with none,
        // which Hibernate rejects outright for a @Modifying query. TransactionTemplate
        // opens one explicitly per thread, mirroring what the real @Transactional
        // service method (PasswordResetTokenService) already provides in production.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Callable<Integer> attempt = () -> transactionTemplate.execute(
                status -> passwordResetTokenRepository.markUsedIfStillValid(tokenId, Instant.now()));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(attempt);
            Future<Integer> second = pool.submit(attempt);

            int totalAffected = first.get() + second.get();
            assertThat(totalAffected).isEqualTo(1);
        } finally {
            pool.shutdown();
        }
    }

    // ── SUPER_ADMIN — same public flow as OWNER (DT §12) ────────────────────────

    @Test
    void superAdmin_forgotAndReset_fullFlowWorks() throws Exception {
        String email = "reset-admin-" + System.nanoTime() + "@example.com";
        createSuperAdminAndLogin(email);

        String rawToken = requestResetToken(email);

        attemptReset(rawToken, "BrandNewAdmin1").andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "BrandNewAdmin1"))))
                .andExpect(status().isOk());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String forgot(String email) throws Exception {
        return mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private ForgotPasswordResponse readForgotResponse(String body) throws Exception {
        return objectMapper.readValue(body, ForgotPasswordResponse.class);
    }

    private String requestResetToken(String email) throws Exception {
        String token = readForgotResponse(forgot(email)).debugToken();
        assertThat(token).as("expected a new token to be created for " + email).isNotBlank();
        return token;
    }

    private org.springframework.test.web.servlet.ResultActions attemptReset(String rawToken, String newPassword) throws Exception {
        return mockMvc.perform(post("/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, newPassword))));
    }

    private PasswordResetToken storedToken(String rawToken) {
        return passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken)).orElseThrow();
    }

    /** created_at is @CreatedDate/updatable=false — Hibernate excludes it from UPDATEs, so
     * backdating it (to simulate "outside the cooldown window") must go through raw JDBC. */
    private void backdateTokenCreatedAt(String rawToken, Instant when) {
        UUID id = storedToken(rawToken).getId();
        jdbc.update("UPDATE password_reset_tokens SET created_at = ? WHERE id = ?", Timestamp.from(when), id);
    }

    private String invitePendingOwner(String adminToken, UUID companyId) throws Exception {
        String email = "pending-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/admin/companies/" + companyId + "/owners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteOwnerRequest("Pending", email))))
                .andExpect(status().isCreated());
        return email;
    }

    private void setCompanyStatus(String adminToken, UUID companyId, String status) throws Exception {
        mockMvc.perform(patch("/admin/companies/" + companyId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCompanyStatusRequest(status))))
                .andExpect(status().isOk());
    }
}
