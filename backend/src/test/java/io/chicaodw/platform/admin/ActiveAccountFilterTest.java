package io.chicaodw.platform.admin;

import io.chicaodw.platform.admin.api.dto.UpdateCompanyStatusRequest;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The central new guarantee of DT-011A.7 §13/§14: a cryptographically valid access
 * token issued *before* a company/user was deactivated must stop working on the very
 * next request — not just after its 15-minute TTL naturally expires.
 */
class ActiveAccountFilterTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired UserRepository userRepository;

    @Test
    void accessTokenIssuedBeforeCompanyDeactivation_isRejectedOnNextRequest() throws Exception {
        var owner = registerOwner();
        String ownerToken = owner.accessToken();

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        String adminToken = createSuperAdminAndLogin();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
        mockMvc.perform(patch("/admin/companies/" + companyId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCompanyStatusRequest("INACTIVE"))))
                .andExpect(status().isOk());

        // Same token, no new login — must be rejected immediately.
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Disabled"));
    }

    @Test
    void accessTokenIssuedBeforeUserDeactivation_isRejectedOnNextRequest() throws Exception {
        var owner = registerOwner();
        String ownerToken = owner.accessToken();

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail(owner.email()).orElseThrow();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Disabled"));
    }

    @Test
    void deactivatedSuperAdmin_isRejectedOnAdminEndpoints() throws Exception {
        String email = "deactivatable-admin-" + System.nanoTime() + "@example.com";
        String token = createSuperAdminAndLogin(email);

        mockMvc.perform(get("/admin/companies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        var superAdmin = userRepository.findByEmail(email).orElseThrow();
        superAdmin.setStatus(UserStatus.INACTIVE);
        userRepository.save(superAdmin);

        mockMvc.perform(get("/admin/companies").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Disabled"));
    }
}
