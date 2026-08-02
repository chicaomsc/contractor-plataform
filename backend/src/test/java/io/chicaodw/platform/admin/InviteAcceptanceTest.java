package io.chicaodw.platform.admin;

import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.admin.api.dto.OwnerInviteResponse;
import io.chicaodw.platform.auth.api.dto.AcceptInviteRequest;
import io.chicaodw.platform.auth.application.InviteService;
import io.chicaodw.platform.auth.domain.OwnerInvite;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.OwnerInviteRepository;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InviteAcceptanceTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired OwnerInviteRepository ownerInviteRepository;
    @Autowired InviteService inviteService;

    private record InvitedOwner(UUID ownerId, String rawToken) {}

    @Test
    void validToken_setsPasswordActivatesOwnerAndAutoLogsIn() throws Exception {
        var invited = invitePendingOwner();

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInviteRequest(invited.rawToken(), "NewPassword1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

        assertThat(userRepository.findById(invited.ownerId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Second use of the same (now consumed) token must fail.
        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInviteRequest(invited.rawToken(), "AnotherPass1"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        var invited = invitePendingOwner();

        OwnerInvite invite = ownerInviteRepository.findByTokenHash(TokenHasher.sha256Hex(invited.rawToken())).orElseThrow();
        invite.setExpiresAt(Instant.now().minusSeconds(60));
        ownerInviteRepository.save(invite);

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInviteRequest(invited.rawToken(), "NewPassword1"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownToken_returnsNotFound() throws Exception {
        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInviteRequest("does-not-exist", "NewPassword1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void weakPassword_returnsBadRequest() throws Exception {
        var invited = invitePendingOwner();

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInviteRequest(invited.rawToken(), "short"))))
                .andExpect(status().isBadRequest());
    }

    /**
     * SEC-AUTH-06/Sprint 11B.6D — two concurrent acceptances of the same invite token
     * must never both succeed. Calls InviteService directly (not through MockMvc) so
     * both threads race against the same real database via the atomic
     * OwnerInviteRepository.markUsedIfStillValid UPDATE.
     */
    @Test
    void concurrentAcceptance_onlyOneWins() throws Exception {
        var invited = invitePendingOwner();
        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    inviteService.acceptInvite(invited.rawToken(), "ConcurrentPass1");
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
            if (result.get()) successes++;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(userRepository.findById(invited.ownerId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private InvitedOwner invitePendingOwner() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var seedOwner = registerOwner();
        UUID companyId = companyRepository.findBySlug(seedOwner.companySlug()).orElseThrow().getId();
        String ownerEmail = "invite-accept-" + System.nanoTime() + "@example.com";

        String body = mockMvc.perform(post("/admin/companies/" + companyId + "/owners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteOwnerRequest("Invited", ownerEmail))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OwnerInviteResponse response = objectMapper.readValue(body, OwnerInviteResponse.class);
        return new InvitedOwner(response.owner().id(), response.invite().token());
    }
}
