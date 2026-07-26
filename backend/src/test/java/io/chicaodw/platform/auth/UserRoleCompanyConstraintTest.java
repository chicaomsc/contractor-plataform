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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

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
