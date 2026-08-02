package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCompanyStatusTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;

    @Test
    void deactivateCompany_blocksLoginAndPublicSite_reactivateRestoresBoth() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        mockMvc.perform(get("/public/sites/" + owner.companySlug()))
                .andExpect(status().isOk());

        setStatus(adminToken, companyId, "INACTIVE");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(owner.email(), owner.password()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Disabled"));

        // 404, not a "this tenant is disabled" message — never reveals it still exists.
        mockMvc.perform(get("/public/sites/" + owner.companySlug()))
                .andExpect(status().isNotFound());

        // SUPER_ADMIN keeps full visibility/administration of the inactive company.
        mockMvc.perform(get("/admin/companies/" + companyId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.status").value("INACTIVE"));

        setStatus(adminToken, companyId, "ACTIVE");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(owner.email(), owner.password()))))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateCompany_blocksRefresh() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();
        UUID companyId = companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(owner.email(), owner.password()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readValue(loginBody, AuthResponse.class).refreshToken();

        setStatus(adminToken, companyId, "INACTIVE");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Disabled"));
    }
}
