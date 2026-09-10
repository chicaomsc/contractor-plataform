package io.chicaodw.platform.auth.domain;

/**
 * {@code SUPER_ADMIN} is the platform administrator — no {@code companyId}, never a
 * tenant, scoped only to {@code /admin/**} (see {@code chk_users_role_company_id}, V11).
 * The other three are tenant roles — every one of them belongs to exactly one Company
 * ({@code companyId != null}) and is subject to the same account/session validation
 * ({@code ActiveAccountFilter}, {@code auth_version}) as any other tenant user; they
 * differ only in which business endpoints {@code @PreAuthorize} lets them reach
 * (DT-017A — see docs/design/DT-017A-roles-authorization-foundation.md for the matrix):
 *
 * <ul>
 *   <li>{@code OWNER} — full tenant control; the only role that can change Company
 *       profile/settings or (in a later phase) manage the team itself.</li>
 *   <li>{@code MANAGER} — day-to-day operational admin: customers, estimates, services,
 *       gallery, branding; not Company profile/settings.</li>
 *   <li>{@code MEMBER} — the most restricted tenant role: customers and estimates only.</li>
 * </ul>
 *
 * Adding {@code MANAGER}/{@code MEMBER} required no migration — {@code users.role} is
 * already {@code VARCHAR(20)} and {@code chk_users_role_company_id} already accepts any
 * non-{@code SUPER_ADMIN} value with a non-null {@code companyId}.
 */
public enum UserRole {
    OWNER,
    SUPER_ADMIN,
    MANAGER,
    MEMBER
}
