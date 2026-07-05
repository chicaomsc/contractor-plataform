package io.chicaodw.platform.auth;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.application.UserService;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private CompanyRepository companyRepository;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setName("Test Corp");
        // unique slug per test run to avoid conflicts between methods
        company.setSlug("test-corp-" + System.nanoTime());
        company.setEmail("corp@example.com");
        company.setCountry("PT");
        company.setStatus(CompanyStatus.ACTIVE);
        companyId = companyRepository.save(company).getId();
    }

    // ── Basic persistence ─────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldPersistUserAndPopulateAuditFields() {
        User saved = userRepository.save(buildUser("alice@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @Transactional
    void shouldFindUserByEmail() {
        userRepository.save(buildUser("bob@example.com"));

        var found = userRepository.findByEmail("bob@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @Transactional
    void shouldReturnUsersForCompany() {
        userRepository.save(buildUser("carol@example.com"));
        userRepository.save(buildUser("dan@example.com"));

        var users = userRepository.findByCompanyId(companyId);

        assertThat(users).hasSizeGreaterThanOrEqualTo(2);
    }

    // ── Email uniqueness — DB constraint ─────────────────────────────────────

    @Test
    void shouldRejectDuplicateEmailAtDatabaseLevel() {
        // unique address to avoid collision with other test methods
        String email = "dup-db-" + System.nanoTime() + "@example.com";

        userRepository.save(buildUser(email));

        assertThatThrownBy(() -> userRepository.saveAndFlush(buildUser(email)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Email uniqueness — service rule ──────────────────────────────────────

    @Test
    void shouldRejectDuplicateEmailAtServiceLevel() {
        String email = "dup-svc-" + System.nanoTime() + "@example.com";

        userService.create(buildUser(email));

        assertThatThrownBy(() -> userService.create(buildUser(email)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(email);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private User buildUser(String email) {
        User user = new User();
        user.setCompanyId(companyId);
        user.setEmail(email);
        user.setPasswordHash("placeholder-hash");
        user.setName("Test User");
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
