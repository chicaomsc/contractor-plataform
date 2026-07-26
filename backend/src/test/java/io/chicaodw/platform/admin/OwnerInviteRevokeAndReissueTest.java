package io.chicaodw.platform.admin;

import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.admin.api.dto.InviteResponse;
import io.chicaodw.platform.admin.api.dto.OwnerInviteResponse;
import io.chicaodw.platform.auth.api.dto.AcceptInviteRequest;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnerInviteRevokeAndReissueTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;

    private record PendingOwner(UUID companyId, UUID ownerId, String rawToken) {}

    @Test
    void reissue_revokesPreviousTokenAndIssuesNewUsableOne() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var pending = invitePendingOwner(adminToken);

        String reissueBody = mockMvc.perform(post("/admin/companies/" + pending.companyId()
                        + "/owners/" + pending.ownerId() + "/invites")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InviteResponse newInvite = objectMapper.readValue(reissueBody, InviteResponse.class);
        assertThat(newInvite.token()).isNotEqualTo(pending.rawToken());

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AcceptInviteRequest(pending.rawToken(), "BrandNewPass1"))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AcceptInviteRequest(newInvite.token(), "BrandNewPass1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void revoke_leavesOwnerWithNoUsableToken() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var pending = invitePendingOwner(adminToken);

        mockMvc.perform(delete("/admin/companies/" + pending.companyId() + "/owners/" + pending.ownerId() + "/invite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AcceptInviteRequest(pending.rawToken(), "BrandNewPass1"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reissueAndRevoke_forAlreadyActiveOwner_returnConflict() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var pending = invitePendingOwner(adminToken);

        mockMvc.perform(post("/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AcceptInviteRequest(pending.rawToken(), "BrandNewPass1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/companies/" + pending.companyId() + "/owners/" + pending.ownerId() + "/invites")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/admin/companies/" + pending.companyId() + "/owners/" + pending.ownerId() + "/invite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    private PendingOwner invitePendingOwner(String adminToken) throws Exception {
        var seedOwner = registerOwner();
        UUID companyId = companyRepository.findBySlug(seedOwner.companySlug()).orElseThrow().getId();
        String ownerEmail = "invited-owner-" + System.nanoTime() + "@example.com";

        String body = mockMvc.perform(post("/admin/companies/" + companyId + "/owners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteOwnerRequest("Invited Owner", ownerEmail))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OwnerInviteResponse response = objectMapper.readValue(body, OwnerInviteResponse.class);
        return new PendingOwner(companyId, response.owner().id(), response.invite().token());
    }
}
