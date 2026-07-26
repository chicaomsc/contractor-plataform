package io.chicaodw.platform.auth.domain;

import io.chicaodw.platform.common.exception.BusinessRuleException;

import java.util.UUID;

/**
 * Application-level check for the SUPER_ADMIN/company_id invariant (DT-011A.7 §5) —
 * an earlier, friendlier-error-message layer. The real, non-bypassable guarantee is
 * the {@code chk_users_role_company_id} CHECK constraint added in V11.
 */
public final class UserRoleInvariant {

    private UserRoleInvariant() {
    }

    public static void validate(UserRole role, UUID companyId) {
        boolean valid = (role == UserRole.SUPER_ADMIN) == (companyId == null);
        if (!valid) {
            throw new BusinessRuleException(
                    "Invalid role/company combination: SUPER_ADMIN must have no company, "
                            + "every other role must belong to a company.");
        }
    }
}
