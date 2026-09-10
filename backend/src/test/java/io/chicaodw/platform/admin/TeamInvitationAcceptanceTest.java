package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.application.TeamInvitationService;
import io.chicaodw.platform.auth.domain.TeamInvitation;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.TeamInvitationRepository;
import io.chicaodw.platform.common.email.EmailService;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-017B — POST /auth/team-invitations/accept. The raw token is deliberately never
 * returned by POST /team/invitations (§23), so every test here captures it the same
 * way {@code PasswordResetEmailFlowTest} does: mock {@link EmailService} and pull the
 * token out of the accept link handed to {@code sendTeamInvitationEmail}.
 */
class TeamInvitationAcceptanceTest extends AbstractAdminIntegrationTest {

    @MockitoBean EmailService emailService;

    @Autowired TeamInvitationService teamInvitationService;
    @Autowired TeamInvitationRepository teamInvitationRepository;
    @Autowired CompanyRepository companyRepository;

    private String acceptBody(String token, String name, String password) {
        return "{\"token\":\"%s\",\"name\":\"%s\",\"password\":\"%s\"}".formatted(token, name, password);
    }

    // ── valid acceptance ─────────────────────────────────────────────────────

    @Test
    void validManagerInvitation_createsActiveManagerUser_andReturnsWorkingSession() throws Exception {
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        String email = "new-manager-" + System.nanoTime() + "@example.com";
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), email, "MANAGER");

        String body = mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "New Manager", "NewManagerPass1")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AuthResponse auth = objectMapper.readValue(body, AuthResponse.class);

        assertThat(auth.user().email()).isEqualTo(email);
        assertThat(auth.user().role()).isEqualTo("MANAGER");
        assertThat(auth.user().companyId()).isEqualTo(companyId);
        assertThat(auth.accessToken()).isNotBlank();
        assertThat(auth.refreshToken()).isNotBlank();

        User created = userRepository.findByEmail(email).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(created.getCompanyId()).isEqualTo(companyId);
        assertThat(passwordEncoder.matches("NewManagerPass1", created.getPasswordHash())).isTrue();

        // DT-017A: the freshly-issued session must work end-to-end as a real MANAGER —
        // services is OWNER+MANAGER per the matrix.
        mockMvc.perform(get("/services").header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void validMemberInvitation_createsActiveMemberUser() throws Exception {
        var owner = registerOwner();
        String email = "new-member-" + System.nanoTime() + "@example.com";
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), email, "MEMBER");

        String body = mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "New Member", "NewMemberPass1")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AuthResponse auth = objectMapper.readValue(body, AuthResponse.class);

        assertThat(auth.user().role()).isEqualTo("MEMBER");
        User created = userRepository.findByEmail(email).orElseThrow();
        assertThat(created.getRole()).isEqualTo(UserRole.MEMBER);

        // DT-017A: MEMBER is barred from /services.
        mockMvc.perform(get("/services").header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isForbidden());
    }

    // ── invalid / expired / revoked / used ──────────────────────────────────

    @Test
    void unknownToken_isRejected_withGenericMessage() throws Exception {
        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody("this-token-never-existed", "Someone", "SomeonePass1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.detail").value("O convite é inválido ou não está mais disponível."));
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        var owner = registerOwner();
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), "expired-" + System.nanoTime() + "@example.com", "MEMBER");
        backdateExpiry(rawToken);

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "Someone", "SomeonePass1")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void revokedToken_isRejected() throws Exception {
        var owner = registerOwner();
        String email = "revoked-accept-" + System.nanoTime() + "@example.com";
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), email, "MEMBER");
        UUID invitationId = teamInvitationRepository.findByTokenHash(
                io.chicaodw.platform.common.security.TokenHasher.sha256Hex(rawToken)).orElseThrow().getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/team/invitations/" + invitationId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "Someone", "SomeonePass1")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reusedToken_secondAcceptanceIsRejected() throws Exception {
        var owner = registerOwner();
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), "reuse-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "First", "FirstPass1")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "Second", "SecondPass1")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void inactiveCompany_isRejected_andCreatesNoUser() throws Exception {
        var owner = registerOwner();
        String email = "inactive-company-" + System.nanoTime() + "@example.com";
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), email, "MEMBER");

        var company = companyRepository.findBySlug(owner.companySlug()).orElseThrow();
        company.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "Someone", "SomeonePass1")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    void emailClaimedAfterInvitationWasIssued_isRejected_andCreatesNoSecondUser() throws Exception {
        var owner = registerOwner();
        String email = "claimed-after-invite-" + System.nanoTime() + "@example.com";
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), email, "MEMBER");

        // Someone else registers with the exact same email before the invite is accepted.
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new io.chicaodw.platform.auth.api.dto.RegisterRequest(
                                        "Someone Else", email, "SomeoneElsePass1", "Other Co " + System.nanoTime(), "PT"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "Someone", "SomeonePass1")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    // ── request validation ───────────────────────────────────────────────────

    @Test
    void invalidPassword_tooShort_returnsBadRequest() throws Exception {
        var owner = registerOwner();
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), "weak-pass-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "Someone", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankName_returnsBadRequest() throws Exception {
        var owner = registerOwner();
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), "blank-name-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(post("/auth/team-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody(rawToken, "", "SomeonePass1")))
                .andExpect(status().isBadRequest());
    }

    // ── concurrency (§16/§30 — CRITICAL) ─────────────────────────────────────

    /**
     * The same guarantee as {@code InviteAcceptanceTest.concurrentAcceptance_onlyOneWins}
     * / {@code LoginIntegrationTest.refresh_concurrentCalls_onlyOneWins}, applied here:
     * two concurrent acceptances of the same team-invitation token must never both
     * succeed. Calls {@link TeamInvitationService} directly (not through MockMvc) so
     * every thread races against the same real database via the atomic
     * {@code TeamInvitationRepository.markUsedIfStillValid} UPDATE.
     */
    @Test
    void concurrentAcceptance_onlyOneWins() throws Exception {
        var owner = registerOwner();
        String email = "concurrent-" + System.nanoTime() + "@example.com";
        String rawToken = createInvitationAndCaptureToken(owner.accessToken(), email, "MEMBER");

        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            int idx = i;
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    teamInvitationService.acceptInvitation(rawToken, "Concurrent " + idx, "ConcurrentPass1");
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();

        long successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(userRepository.findByEmail(email)).isPresent();
        TeamInvitation invitation = teamInvitationRepository.findByTokenHash(
                io.chicaodw.platform.common.security.TokenHasher.sha256Hex(rawToken)).orElseThrow();
        assertThat(invitation.getUsedAt()).isNotNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String createInvitationAndCaptureToken(String ownerToken, String email, String role) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}";
        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        var linkCaptor = forClass(String.class);
        verify(emailService).sendTeamInvitationEmail(anyString(), anyString(), anyString(), linkCaptor.capture(), any());
        String link = linkCaptor.getValue();
        String marker = "#token=";
        return link.substring(link.indexOf(marker) + marker.length());
    }

    private void backdateExpiry(String rawToken) {
        TeamInvitation invitation = teamInvitationRepository.findByTokenHash(
                io.chicaodw.platform.common.security.TokenHasher.sha256Hex(rawToken)).orElseThrow();
        invitation.setExpiresAt(Instant.now().minusSeconds(60));
        teamInvitationRepository.save(invitation);
    }
}
