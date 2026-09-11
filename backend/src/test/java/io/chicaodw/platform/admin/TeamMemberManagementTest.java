package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.auth.api.dto.TeamMemberResponse;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-017C — GET/PATCH/DELETE on /team/members from the OWNER's perspective. Pure
 * authorization (MANAGER/MEMBER/SUPER_ADMIN → 403) lives in
 * {@code TeamRoleAuthorizationMatrixTest}; concurrency lives in
 * {@code TeamMemberConcurrencyTest}. This class covers the domain behavior and the
 * session-invalidation guarantees.
 */
class TeamMemberManagementTest extends AbstractAdminIntegrationTest {

    @Autowired CompanyRepository companyRepository;

    private UUID companyId(RegisteredOwner owner) {
        return companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
    }

    private String roleBody(String role) {
        return "{\"role\":\"" + role + "\"}";
    }

    // ── GET /team/members ─────────────────────────────────────────────────────

    @Test
    void list_returnsOwnerManagerAndMember_ofOwnCompany_ownerFirst() throws Exception {
        var owner = registerOwner();
        UUID cid = companyId(owner);
        addMember(cid, UserRole.MEMBER);
        addMember(cid, UserRole.MANAGER);

        String body = mockMvc.perform(get("/team/members").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<TeamMemberResponse> members = List.of(objectMapper.readValue(body, TeamMemberResponse[].class));
        assertThat(members).hasSize(3);
        assertThat(members).extracting(TeamMemberResponse::role)
                .containsExactly("OWNER", "MANAGER", "MEMBER"); // deterministic order
        assertThat(body).doesNotContain("passwordHash").doesNotContain("authVersion")
                .doesNotContain("tokenHash").doesNotContain("refreshToken");
    }

    @Test
    void list_neverContainsAnotherCompanysMembers() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();
        addMember(companyId(ownerB), UserRole.MANAGER);

        String body = mockMvc.perform(get("/team/members").header("Authorization", "Bearer " + ownerA.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<TeamMemberResponse> members = List.of(objectMapper.readValue(body, TeamMemberResponse[].class));
        assertThat(members).hasSize(1); // only ownerA
        assertThat(members.get(0).role()).isEqualTo("OWNER");
    }

    @Test
    void list_includesARemovedMemberAsInactive() throws Exception {
        var owner = registerOwner();
        UUID cid = companyId(owner);
        var member = addMember(cid, UserRole.MEMBER);

        mockMvc.perform(delete("/team/members/" + member.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        String body = mockMvc.perform(get("/team/members").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<TeamMemberResponse> members = List.of(objectMapper.readValue(body, TeamMemberResponse[].class));
        assertThat(members).filteredOn(m -> m.id().equals(member.userId()))
                .singleElement()
                .satisfies(m -> assertThat(m.status()).isEqualTo("INACTIVE"));
    }

    // ── PATCH /team/members/{id}/role ─────────────────────────────────────────

    @Test
    void changeRole_managerToMember_persistsAndInvalidatesTheTargetsSessions() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MANAGER);
        long authVersionBefore = userRepository.findById(member.userId()).orElseThrow().getAuthVersion();

        // the member's own token works before the change
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/team/members/" + member.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.id").value(member.userId().toString()));

        User after = userRepository.findById(member.userId()).orElseThrow();
        assertThat(after.getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(after.getAuthVersion()).isGreaterThan(authVersionBefore);

        // old access token → rejected immediately (auth_version mismatch)
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isUnauthorized());
        // old refresh token → rejected
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(member.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());

        // a fresh login still works and now carries the new role: MEMBER is barred from /services (DT-017A)
        String freshToken = login(member.email(), member.password());
        mockMvc.perform(get("/services").header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRole_memberToManager_succeeds() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);

        mockMvc.perform(patch("/team/members/" + member.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        assertThat(userRepository.findById(member.userId()).orElseThrow().getRole()).isEqualTo(UserRole.MANAGER);
        String freshToken = login(member.email(), member.password());
        mockMvc.perform(get("/services").header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isOk()); // MANAGER can access services
    }

    @Test
    void changeRole_sameRole_isIdempotent_noSessionInvalidation() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MANAGER);
        long authVersionBefore = userRepository.findById(member.userId()).orElseThrow().getAuthVersion();

        mockMvc.perform(patch("/team/members/" + member.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        assertThat(userRepository.findById(member.userId()).orElseThrow().getAuthVersion()).isEqualTo(authVersionBefore);
        // the member's original token is still valid — nothing was invalidated
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void changeRole_targetIsAnOwner_isRejected() throws Exception {
        var owner = registerOwner();
        // a second OWNER in the same company (allowed by the existing platform-admin flow)
        var secondOwner = addMember(companyId(owner), UserRole.MANAGER);
        User asOwner = userRepository.findById(secondOwner.userId()).orElseThrow();
        asOwner.setRole(UserRole.OWNER);
        userRepository.save(asOwner);

        mockMvc.perform(patch("/team/members/" + secondOwner.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("MEMBER")))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(secondOwner.userId()).orElseThrow().getRole()).isEqualTo(UserRole.OWNER);
    }

    @Test
    void changeRole_toOwner_isRejectedAsInvalidRole() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);

        mockMvc.perform(patch("/team/members/" + member.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("OWNER")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeRole_toSuperAdmin_isRejectedAsInvalidRole() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);

        mockMvc.perform(patch("/team/members/" + member.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("SUPER_ADMIN")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeRole_garbageRole_isRejected() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);

        mockMvc.perform(patch("/team/members/" + member.userId() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("NOT_A_ROLE")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeRole_crossTenantTarget_isNotFound() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();
        var memberB = addMember(companyId(ownerB), UserRole.MANAGER);

        mockMvc.perform(patch("/team/members/" + memberB.userId() + "/role")
                        .header("Authorization", "Bearer " + ownerA.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("MEMBER")))
                .andExpect(status().isNotFound());

        assertThat(userRepository.findById(memberB.userId()).orElseThrow().getRole()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void changeRole_unknownTarget_isNotFound() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(patch("/team/members/" + UUID.randomUUID() + "/role")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody("MEMBER")))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /team/members/{id} ────────────────────────────────────────────

    @Test
    void remove_manager_softDisablesAndInvalidatesSessions_andBlocksFutureLogin() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MANAGER);
        long authVersionBefore = userRepository.findById(member.userId()).orElseThrow().getAuthVersion();

        mockMvc.perform(delete("/team/members/" + member.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        User after = userRepository.findById(member.userId()).orElseThrow(); // still exists — no physical delete
        assertThat(after.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(after.getAuthVersion()).isGreaterThan(authVersionBefore);

        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(member.refreshToken()))))
                .andExpect(status().isUnprocessableEntity());
        // future login is blocked while INACTIVE
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + member.email() + "\",\"password\":\"" + member.password() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void remove_member_softDisables() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);

        mockMvc.perform(delete("/team/members/" + member.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(member.userId()).orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void remove_alreadyInactiveMember_isIdempotent_noSecondAuthVersionBump() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);

        mockMvc.perform(delete("/team/members/" + member.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());
        long authVersionAfterFirst = userRepository.findById(member.userId()).orElseThrow().getAuthVersion();

        mockMvc.perform(delete("/team/members/" + member.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(member.userId()).orElseThrow().getAuthVersion()).isEqualTo(authVersionAfterFirst);
    }

    @Test
    void remove_targetIsAnOwner_isRejected() throws Exception {
        var owner = registerOwner();
        var secondOwner = addMember(companyId(owner), UserRole.MANAGER);
        User asOwner = userRepository.findById(secondOwner.userId()).orElseThrow();
        asOwner.setRole(UserRole.OWNER);
        userRepository.save(asOwner);

        mockMvc.perform(delete("/team/members/" + secondOwner.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(secondOwner.userId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void remove_self_isRejected_becauseCallerIsAlwaysAnOwner() throws Exception {
        var owner = registerOwner();
        UUID ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();

        mockMvc.perform(delete("/team/members/" + ownerId).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(ownerId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void remove_crossTenantTarget_isNotFound() throws Exception {
        var ownerA = registerOwner();
        var ownerB = registerOwner();
        var memberB = addMember(companyId(ownerB), UserRole.MEMBER);

        mockMvc.perform(delete("/team/members/" + memberB.userId()).header("Authorization", "Bearer " + ownerA.accessToken()))
                .andExpect(status().isNotFound());

        assertThat(userRepository.findById(memberB.userId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void remove_unknownTarget_isNotFound() throws Exception {
        var owner = registerOwner();

        mockMvc.perform(delete("/team/members/" + UUID.randomUUID()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNotFound());
    }

    // ── company disabled ─────────────────────────────────────────────────────

    @Test
    void manage_whenOwnersCompanyIsInactive_isRejectedByActiveAccountFilter() throws Exception {
        var owner = registerOwner();
        var member = addMember(companyId(owner), UserRole.MEMBER);
        String adminToken = createSuperAdminAndLogin();
        setStatus(adminToken, companyId(owner), "INACTIVE");

        mockMvc.perform(get("/team/members").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/team/members/" + member.userId()).header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isUnauthorized());
    }
}
