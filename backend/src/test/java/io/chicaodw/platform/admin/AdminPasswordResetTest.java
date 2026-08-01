package io.chicaodw.platform.admin;

import io.chicaodw.platform.admin.api.dto.AdminPasswordResetResponse;
import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.admin.api.dto.OwnerInviteResponse;
import io.chicaodw.platform.auth.api.dto.ResetPasswordRequest;
import io.chicaodw.platform.auth.domain.PasswordResetToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.PasswordResetTokenRepository;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /admin/companies/{companyId}/owners/{ownerId}/password-reset — DT-011A.10 §13/§15/§17. */
class AdminPasswordResetTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void generateLink_forActiveOwner_returnsUsableLink() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();

        AdminPasswordResetResponse response = generateLink(adminToken, companyId, ownerId);

        assertThat(response.resetLink()).contains("/reset-password#token=");
        assertThat(response.expiresAt()).isAfter(java.time.Instant.now());

        String rawToken = tokenFromLink(response.resetLink());
        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "AdminIssuedPass1"))))
                .andExpect(status().isOk());
    }

    @Test
    void generateLink_revokesPreviousStillValidToken() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();

        String firstToken = tokenFromLink(generateLink(adminToken, companyId, ownerId).resetLink());
        generateLink(adminToken, companyId, ownerId);

        PasswordResetToken stored = passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256Hex(firstToken)).orElseThrow();
        assertThat(stored.getRevokedAt()).isNotNull();

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(firstToken, "AdminIssuedPass1"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void generateLink_forPendingOwner_returnsConflict() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var seedOwner = registerOwner();
        UUID companyId = companyRepository.findBySlug(seedOwner.companySlug()).orElseThrow().getId();

        String pendingEmail = "pending-admin-reset-" + System.nanoTime() + "@example.com";
        String inviteBody = mockMvc.perform(post("/admin/companies/" + companyId + "/owners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteOwnerRequest("Pending", pendingEmail))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID pendingOwnerId = objectMapper.readValue(inviteBody, OwnerInviteResponse.class).owner().id();

        mockMvc.perform(post("/admin/companies/" + companyId + "/owners/" + pendingOwnerId + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void generateLink_forInactiveOwner_returnsConflict() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        User user = userRepository.findById(ownerId).orElseThrow();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        mockMvc.perform(post("/admin/companies/" + companyId + "/owners/" + ownerId + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void generateLink_forOwnerOfAnotherCompany_returnsNotFound() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var ownerA = registerOwner();
        var ownerB = registerOwner();
        UUID companyAId = companyRepository.findBySlug(ownerA.companySlug()).orElseThrow().getId();
        UUID ownerBId = userRepository.findByEmail(ownerB.email()).orElseThrow().getId();

        mockMvc.perform(post("/admin/companies/" + companyAId + "/owners/" + ownerBId + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateLink_forUnknownCompany_returnsNotFound() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();

        mockMvc.perform(post("/admin/companies/" + UUID.randomUUID() + "/owners/" + ownerId + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateLink_targetingSuperAdmin_returnsNotFound_structurallyImpossible() throws Exception {
        String superAdminEmail = "target-admin-" + System.nanoTime() + "@example.com";
        String adminToken = createSuperAdminAndLogin(superAdminEmail);
        UUID targetSuperAdminId = userRepository.findByEmail(superAdminEmail).orElseThrow().getId();

        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        mockMvc.perform(post("/admin/companies/" + companyId + "/owners/" + targetSuperAdminId + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void owner_isForbiddenFromGeneratingResetLinks() throws Exception {
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();

        mockMvc.perform(post("/admin/companies/" + companyId + "/owners/" + ownerId + "/password-reset")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isForbidden());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AdminPasswordResetResponse generateLink(String adminToken, UUID companyId, UUID ownerId) throws Exception {
        String body = mockMvc.perform(post("/admin/companies/" + companyId + "/owners/" + ownerId + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AdminPasswordResetResponse.class);
    }

    private String tokenFromLink(String resetLink) {
        return resetLink.substring(resetLink.indexOf("token=") + "token=".length());
    }
}
