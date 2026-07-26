-- Sprint 11A.7 — Platform administration & multi-tenancy: SUPER_ADMIN role support.
--
-- User.company_id becomes nullable so a SUPER_ADMIN (platform staff, no company) can
-- exist in the same `users` table as OWNER accounts — see
-- docs/design/DT-011A.7-platform-admin-multitenancy.md §5. The CHECK constraint below
-- is the enforced invariant (application-level validation in UserRoleInvariant is a
-- second, earlier layer for a better error message — this constraint is the source of
-- truth): SUPER_ADMIN rows always have company_id NULL, every other role always has
-- company_id NOT NULL. Existing OWNER rows (all with company_id already set) already
-- satisfy this constraint — no backfill needed.

ALTER TABLE users ALTER COLUMN company_id DROP NOT NULL;

ALTER TABLE users ADD CONSTRAINT chk_users_role_company_id
    CHECK (
        (role = 'SUPER_ADMIN' AND company_id IS NULL)
        OR
        (role <> 'SUPER_ADMIN' AND company_id IS NOT NULL)
    );

-- `status` already stores UserStatus as a plain VARCHAR (no native Postgres enum
-- type), so adding the new PENDING value (invited, awaiting password) needs no DDL
-- change here — same reasoning applies to `role` gaining SUPER_ADMIN.
