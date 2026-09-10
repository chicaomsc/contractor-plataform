package io.chicaodw.platform.auth;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves chk_users_role_company_id (V11) is the real, non-bypassable guarantee — the
 * application-level UserRoleInvariant check is only a friendlier earlier layer.
 */
class UserRoleCompanyConstraintTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;

    @Test
    void superAdminWithCompanyId_violatesDatabaseConstraint() {
        UUID companyId = createCompany();

        User badSuperAdmin = new User();
        badSuperAdmin.setCompanyId(companyId);
        badSuperAdmin.setEmail("bad-super-admin-" + System.nanoTime() + "@example.com");
        badSuperAdmin.setPasswordHash("hash");
        badSuperAdmin.setName("Bad Super Admin");
        badSuperAdmin.setRole(UserRole.SUPER_ADMIN);
        badSuperAdmin.setStatus(UserStatus.ACTIVE);

        assertThatThrownBy(() -> userRepository.saveAndFlush(badSuperAdmin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ownerWithoutCompanyId_violatesDatabaseConstraint() {
        User badOwner = new User();
        badOwner.setCompanyId(null);
        badOwner.setEmail("bad-owner-" + System.nanoTime() + "@example.com");
        badOwner.setPasswordHash("hash");
        badOwner.setName("Bad Owner");
        badOwner.setRole(UserRole.OWNER);
        badOwner.setStatus(UserStatus.ACTIVE);

        assertThatThrownBy(() -> userRepository.saveAndFlush(badOwner))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * DT-017A — confirms the audit's central claim with a real database, not just
     * reasoning about the constraint's SQL text: MANAGER/MEMBER need no migration.
     * chk_users_role_company_id already accepts any role literal other than
     * 'SUPER_ADMIN' paired with a non-null company_id — this persists an actual row
     * with each new role and a real company and asserts it succeeds outright.
     */
    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"MANAGER", "MEMBER"})
    void tenantRole_withCompanyId_isAcceptedByDatabaseConstraint_noMigrationNeeded(UserRole role) {
        UUID companyId = createCompany();

        User user = new User();
        user.setCompanyId(companyId);
        user.setEmail("new-role-" + role.name().toLowerCase() + "-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        user.setName("New Role User");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        assertThatCode(() -> userRepository.saveAndFlush(user)).doesNotThrowAnyException();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(role);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"MANAGER", "MEMBER"})
    void tenantRole_withoutCompanyId_violatesDatabaseConstraint(UserRole role) {
        User user = new User();
        user.setCompanyId(null);
        user.setEmail("bad-" + role.name().toLowerCase() + "-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        user.setName("Bad " + role.name());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID createCompany() {
        Company company = new Company();
        company.setName("Constraint Co");
        company.setSlug("constraint-co-" + System.nanoTime());
        company.setEmail("constraint@example.com");
        company.setCountry("PT");
        company.setStatus(CompanyStatus.ACTIVE);
        return companyRepository.save(company).getId();
    }
}
