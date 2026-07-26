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
}
