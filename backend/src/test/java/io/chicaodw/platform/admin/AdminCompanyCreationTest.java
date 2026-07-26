package io.chicaodw.platform.admin;

import io.chicaodw.platform.admin.api.dto.CompanyOnboardingResponse;
import io.chicaodw.platform.admin.api.dto.CreateCompanyRequest;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.OwnerInviteRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCompanyCreationTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired OwnerInviteRepository ownerInviteRepository;

    @Test
    void createCompany_createsCompanyPendingOwnerAndHashedInvite() throws Exception {
        String token = createSuperAdminAndLogin();
        String ownerEmail = "new-owner-" + System.nanoTime() + "@example.com";

        CreateCompanyRequest request = new CreateCompanyRequest(
                "New Co " + System.nanoTime(), null, "PT", null, "New Owner", ownerEmail);

        String body = mockMvc.perform(post("/admin/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company.status").value("ACTIVE"))
                .andExpect(jsonPath("$.owner.email").value(ownerEmail))
                .andExpect(jsonPath("$.owner.status").value("PENDING"))
                .andExpect(jsonPath("$.invite.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        CompanyOnboardingResponse response = objectMapper.readValue(body, CompanyOnboardingResponse.class);

        var owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        assertThat(owner.getStatus()).isEqualTo(UserStatus.PENDING);

        var validInvites = ownerInviteRepository
                .findByUserIdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(owner.getId(), Instant.now());
        assertThat(validInvites).hasSize(1);
        // The raw token is never persisted — only its SHA-256 hash (64 hex chars).
        assertThat(validInvites.get(0).getTokenHash())
                .hasSize(64)
                .isNotEqualTo(response.invite().token());
    }

    @Test
    void createCompany_duplicateOwnerEmail_rejectedWithNothingCreated() throws Exception {
        String token = createSuperAdminAndLogin();
        var existingOwner = registerOwner();
        long companiesBefore = companyRepository.count();

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Dup Email Co " + System.nanoTime(), null, "PT", null, "Dup Owner", existingOwner.email());

        mockMvc.perform(post("/admin/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business Rule Violation"));

        // All-or-nothing outcome (DT-011A.7 §10) — the rejected attempt created no Company.
        assertThat(companyRepository.count()).isEqualTo(companiesBefore);
    }

    @Test
    void createCompany_duplicateSlug_returnsUnprocessableEntity() throws Exception {
        String token = createSuperAdminAndLogin();
        var existing = registerOwner();

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Another Co " + System.nanoTime(), existing.companySlug(), "PT", null, "Owner Two",
                "owner-two-" + System.nanoTime() + "@example.com");

        mockMvc.perform(post("/admin/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
