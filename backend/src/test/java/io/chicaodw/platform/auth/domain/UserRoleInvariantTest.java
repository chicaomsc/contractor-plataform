package io.chicaodw.platform.auth.domain;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRoleInvariantTest {

    @Test
    void superAdminWithoutCompany_isValid() {
        assertThatCode(() -> UserRoleInvariant.validate(UserRole.SUPER_ADMIN, null))
                .doesNotThrowAnyException();
    }

    @Test
    void ownerWithCompany_isValid() {
        assertThatCode(() -> UserRoleInvariant.validate(UserRole.OWNER, UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void superAdminWithCompany_isRejected() {
        assertThatThrownBy(() -> UserRoleInvariant.validate(UserRole.SUPER_ADMIN, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void ownerWithoutCompany_isRejected() {
        assertThatThrownBy(() -> UserRoleInvariant.validate(UserRole.OWNER, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // DT-017A — MANAGER/MEMBER are tenant roles exactly like OWNER as far as this
    // invariant is concerned: the check is already role-agnostic (only branches on
    // SUPER_ADMIN vs. everything else), so these are regression tests confirming that
    // genericity held after adding the two new enum values, not a behavior change.

    @Test
    void managerWithCompany_isValid() {
        assertThatCode(() -> UserRoleInvariant.validate(UserRole.MANAGER, UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void managerWithoutCompany_isRejected() {
        assertThatThrownBy(() -> UserRoleInvariant.validate(UserRole.MANAGER, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void memberWithCompany_isValid() {
        assertThatCode(() -> UserRoleInvariant.validate(UserRole.MEMBER, UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void memberWithoutCompany_isRejected() {
        assertThatThrownBy(() -> UserRoleInvariant.validate(UserRole.MEMBER, null))
                .isInstanceOf(BusinessRuleException.class);
    }
}
