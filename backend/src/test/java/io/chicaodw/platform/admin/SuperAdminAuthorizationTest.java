package io.chicaodw.platform.admin;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuperAdminAuthorizationTest extends AbstractAdminIntegrationTest {

    @Test
    void superAdmin_canAccessAdminCompanies() throws Exception {
        String token = createSuperAdminAndLogin();

        mockMvc.perform(get("/admin/companies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void owner_isForbiddenFromAdminCompanies() throws Exception {
        String ownerToken = registerOwnerAndLogin();

        mockMvc.perform(get("/admin/companies").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/companies"))
                .andExpect(status().isUnauthorized());
    }
}
