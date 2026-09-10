package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.api.dto.TeamInvitationResponse;
import io.chicaodw.platform.auth.domain.TeamInvitation;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.infrastructure.persistence.TeamInvitationRepository;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-017B — POST/GET/DELETE/resend on /team/invitations, exercised as the OWNER of the
 * company being invited into. Authorization (OWNER-only, cross-tenant 404) lives in
 * {@code TeamRoleAuthorizationMatrixTest} instead — this class is about the invitation
 * domain behavior itself. Acceptance is covered separately in
 * {@code TeamInvitationAcceptanceTest}.
 */
class TeamInvitationFlowTest extends AbstractAdminIntegrationTest {

    @Autowired TeamInvitationRepository teamInvitationRepository;
    @Autowired CompanyRepository companyRepository;

    private String createBody(String email, String role) {
        return "{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}";
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_managerInvitation_succeeds() throws Exception {
        var owner = registerOwner();
        String email = "manager-" + System.nanoTime() + "@example.com";

        String body = mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, "MANAGER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        TeamInvitationResponse response = objectMapper.readValue(body, TeamInvitationResponse.class);
        assertThat(response.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void create_memberInvitation_succeeds() throws Exception {
        var owner = registerOwner();
        String email = "member-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, "MEMBER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void create_ownerRole_isRejected() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("someone-" + System.nanoTime() + "@example.com", "OWNER")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_superAdminRole_isRejected() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("someone-" + System.nanoTime() + "@example.com", "SUPER_ADMIN")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_garbageRole_isRejected() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("someone-" + System.nanoTime() + "@example.com", "NOT_A_ROLE")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_emailAlreadyBelongsToAUser_isRejected_withGenericMessage() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        // The OWNER's own email already belongs to a User.
                        .content(createBody(owner.email(), "MEMBER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este e-mail já está associado a uma conta."));
    }

    @Test
    void create_duplicatePendingInvitation_isRejected() throws Exception {
        var owner = registerOwner();
        String email = "dup-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, "MEMBER")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, "MANAGER")))
                .andExpect(status().isConflict());
    }

    // NOTE (DT-017B): TeamInvitationService.createInvitation() does call email.trim()
    // before persisting — kept as harmless defensive hygiene, same instinct as
    // AdminCompanyService/AuthService not doing it either — but it is not independently
    // testable through this HTTP endpoint: @Email on CreateTeamInvitationRequest already
    // rejects a string with leading/trailing whitespace as "not a valid email address"
    // (400) before the service's own trim() ever runs, so there is no reachable input
    // for which trimming changes the outcome. No test asserts it directly for that
    // reason — asserting behavior the framework already makes unreachable would be
    // exactly the kind of fragile test the DT-017B spec asks not to add.

    @Test
    void create_persistsCorrectCompanyIdAndInvitedByUserId() throws Exception {
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        String email = "attribution-" + System.nanoTime() + "@example.com";

        String body = mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, "MEMBER")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID invitationId = objectMapper.readValue(body, TeamInvitationResponse.class).id();

        TeamInvitation stored = teamInvitationRepository.findById(invitationId).orElseThrow();
        assertThat(stored.getCompanyId()).isEqualTo(companyId);
        assertThat(stored.getRole()).isEqualTo(UserRole.MEMBER);
        // invitedByUserId can't be asserted against a known id without another lookup,
        // but it must never be null/blank — the field is NOT NULL at the DB level too.
        assertThat(stored.getInvitedByUserId()).isNotNull();
    }

    @Test
    void create_rawTokenIsNeverPersisted_onlyItsHash() throws Exception {
        var owner = registerOwner();
        String email = "hash-only-" + System.nanoTime() + "@example.com";

        String body = mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, "MEMBER")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID invitationId = objectMapper.readValue(body, TeamInvitationResponse.class).id();

        // The response body must not contain a raw token field at all (DT-017B §23).
        assertThat(body).doesNotContain("\"token\"").doesNotContain("\"rawToken\"");

        TeamInvitation stored = teamInvitationRepository.findById(invitationId).orElseThrow();
        assertThat(stored.getTokenHash()).hasSize(64); // SHA-256 hex
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsOnlyOwnCompanyInvitations() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + ownerA.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("a-" + System.nanoTime() + "@example.com", "MEMBER")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + ownerB.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("b-" + System.nanoTime() + "@example.com", "MEMBER")))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(get("/team/invitations")
                        .header("Authorization", "Bearer " + ownerA.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<TeamInvitationResponse> invitations = List.of(objectMapper.readValue(body, TeamInvitationResponse[].class));
        assertThat(invitations).hasSize(1);
        assertThat(body).doesNotContain("\"tokenHash\"");
    }

    // ── revoke ───────────────────────────────────────────────────────────────

    @Test
    void revoke_pendingInvitation_marksItRevoked() throws Exception {
        var owner = registerOwner();
        UUID invitationId = createInvitation(owner.accessToken(), "revoke-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(delete("/team/invitations/" + invitationId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(teamInvitationRepository.findById(invitationId).orElseThrow().getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_alreadyRevokedInvitation_isIdempotent() throws Exception {
        var owner = registerOwner();
        UUID invitationId = createInvitation(owner.accessToken(), "revoke-twice-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(delete("/team/invitations/" + invitationId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/team/invitations/" + invitationId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void revoke_crossTenantInvitation_isNotFound() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();
        UUID invitationInB = createInvitation(ownerB.accessToken(), "cross-revoke-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(delete("/team/invitations/" + invitationInB)
                        .header("Authorization", "Bearer " + ownerA.accessToken()))
                .andExpect(status().isNotFound());
    }

    // ── resend ───────────────────────────────────────────────────────────────

    @Test
    void resend_revokesOldInvitationAndIssuesANewOneWithARenewedExpiry() throws Exception {
        var owner = registerOwner();
        String email = "resend-" + System.nanoTime() + "@example.com";
        UUID oldInvitationId = createInvitation(owner.accessToken(), email, "MANAGER");
        TeamInvitation oldBefore = teamInvitationRepository.findById(oldInvitationId).orElseThrow();
        assertThat(oldBefore.getRevokedAt()).isNull();

        String body = mockMvc.perform(post("/team/invitations/" + oldInvitationId + "/resend")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andReturn().getResponse().getContentAsString();
        TeamInvitationResponse resent = objectMapper.readValue(body, TeamInvitationResponse.class);

        assertThat(resent.id()).isNotEqualTo(oldInvitationId);
        assertThat(teamInvitationRepository.findById(oldInvitationId).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(teamInvitationRepository.findById(resent.id()).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void resend_crossTenantInvitation_isNotFound() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();
        UUID invitationInB = createInvitation(ownerB.accessToken(), "cross-resend-" + System.nanoTime() + "@example.com", "MEMBER");

        mockMvc.perform(post("/team/invitations/" + invitationInB + "/resend")
                        .header("Authorization", "Bearer " + ownerA.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void resend_alreadyAcceptedInvitation_isRejected() throws Exception {
        var owner = registerOwner();
        UUID invitationId = createInvitation(owner.accessToken(), "already-used-" + System.nanoTime() + "@example.com", "MEMBER");
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId).orElseThrow();
        invitation.setUsedAt(Instant.now());
        teamInvitationRepository.save(invitation);

        mockMvc.perform(post("/team/invitations/" + invitationId + "/resend")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isConflict());
    }

    // ── company inactive at create time ─────────────────────────────────────

    @Test
    void create_whenCompanyBecomesInactive_ownerAccessAlreadyRejectedByActiveAccountFilter() throws Exception {
        // Not a TeamInvitationService-specific behavior — ActiveAccountFilter already
        // rejects the OWNER's own token once their Company is INACTIVE (DT-011A.7 §13),
        // before /team/invitations is ever reached. Included here as a regression
        // guard specific to this new controller.
        var owner = registerOwner();
        var company = companyRepository.findBySlug(owner.companySlug()).orElseThrow();
        company.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);

        mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("irrelevant-" + System.nanoTime() + "@example.com", "MEMBER")))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID createInvitation(String ownerToken, String email, String role) throws Exception {
        String body = mockMvc.perform(post("/team/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TeamInvitationResponse.class).id();
    }
}
