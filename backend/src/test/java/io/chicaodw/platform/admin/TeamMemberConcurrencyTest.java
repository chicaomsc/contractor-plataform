package io.chicaodw.platform.admin;

import io.chicaodw.platform.auth.application.TeamMemberService;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DT-017C §22/§23 — concurrency. Both operations are single-statement atomic UPDATEs
 * ({@code auth_version + 1} computed in-database), so:
 *  - two concurrent role changes cannot lose an auth_version increment (no
 *    read-modify-write), and the final role is always one of the requested values; and
 *  - a role change racing a removal can never end with "user INACTIVE but an old
 *    session still valid" — whichever runs, if a removal happened at all the user is
 *    INACTIVE with a bumped auth_version and revoked refresh tokens, so
 *    ActiveAccountFilter rejects every old token regardless of the final role.
 * Drives {@link TeamMemberService} directly (like {@code InviteAcceptanceTest} /
 * {@code TeamInvitationAcceptanceTest}) so the threads race the real database.
 */
class TeamMemberConcurrencyTest extends AbstractAdminIntegrationTest {

    @Autowired TeamMemberService teamMemberService;
    @Autowired CompanyRepository companyRepository;

    private UUID companyId(RegisteredOwner owner) {
        return companyRepository.findBySlug(owner.companySlug()).orElseThrow().getId();
    }

    @Test
    void concurrentIdenticalRoleChanges_endInAValidStateWithoutLosingAuthVersion() throws Exception {
        var owner = registerOwner();
        UUID cid = companyId(owner);
        var member = addMember(cid, UserRole.MANAGER);
        long authVersionBefore = userRepository.findById(member.userId()).orElseThrow().getAuthVersion();

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
                    teamMemberService.changeRole(cid, member.userId(), "MEMBER");
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();
        for (Future<Boolean> r : results) {
            r.get();
        }
        pool.shutdown();

        User after = userRepository.findById(member.userId()).orElseThrow();
        assertThat(after.getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(after.getAuthVersion()).isGreaterThan(authVersionBefore); // at least one bump, none lost

        // the pre-change token must be dead
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removalRacingRoleChange_neverEndsInactiveWithAValidOldSession() throws Exception {
        var owner = registerOwner();
        UUID cid = companyId(owner);
        var member = addMember(cid, UserRole.MEMBER);

        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            boolean remove = (i % 2 == 0);
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    if (remove) {
                        teamMemberService.removeMember(cid, member.userId());
                    } else {
                        teamMemberService.changeRole(cid, member.userId(), "MANAGER");
                    }
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();
        for (Future<Boolean> r : results) {
            r.get();
        }
        pool.shutdown();

        User after = userRepository.findById(member.userId()).orElseThrow();
        // at least one removal ran, so the account must be INACTIVE...
        assertThat(after.getStatus()).isEqualTo(UserStatus.INACTIVE);
        // ...and the old session must be dead no matter the final role
        mockMvc.perform(get("/company/me").header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isUnauthorized());
    }
}
