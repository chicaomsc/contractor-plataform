package io.chicaodw.platform.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.admin.api.dto.UpdateCompanyStatusRequest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for admin/tenant/invite integration tests (DT-011A.7). */
@AutoConfigureMockMvc
abstract class AbstractAdminIntegrationTest extends AbstractIntegrationTest {

    static final String SUPER_ADMIN_PASSWORD = "SuperAdminPass1";
    static final String OWNER_PASSWORD = "OwnerPassword1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    record RegisteredOwner(String email, String password, String companySlug, String accessToken, String refreshToken) {}

    /** Bypasses PlatformAdminBootstrapRunner (env vars aren't set in tests) by inserting the row directly. */
    String createSuperAdminAndLogin() throws Exception {
        return createSuperAdminAndLogin("super-admin-" + System.nanoTime() + "@example.com");
    }

    String createSuperAdminAndLogin(String email) throws Exception {
        User superAdmin = new User();
        superAdmin.setCompanyId(null);
        superAdmin.setEmail(email);
        superAdmin.setPasswordHash(passwordEncoder.encode(SUPER_ADMIN_PASSWORD));
        superAdmin.setName("Test Super Admin");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);
        userRepository.save(superAdmin);

        return login(email, SUPER_ADMIN_PASSWORD);
    }

    RegisteredOwner registerOwner() throws Exception {
        String email = "owner-" + System.nanoTime() + "@example.com";
        String companyName = "Owner Co " + System.nanoTime();
        RegisterRequest req = new RegisterRequest("Owner Test", email, OWNER_PASSWORD, companyName, "PT");

        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AuthResponse response = objectMapper.readValue(body, AuthResponse.class);
        return new RegisteredOwner(
                email, OWNER_PASSWORD, response.company().slug(), response.accessToken(), response.refreshToken());
    }

    String registerOwnerAndLogin() throws Exception {
        return registerOwner().accessToken();
    }

    String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class).accessToken();
    }

    void setStatus(String adminToken, UUID companyId, String status) throws Exception {
        mockMvc.perform(patch("/admin/companies/" + companyId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCompanyStatusRequest(status))))
                .andExpect(status().isOk());
    }
}
