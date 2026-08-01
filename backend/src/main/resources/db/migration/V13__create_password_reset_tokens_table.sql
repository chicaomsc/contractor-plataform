-- Sprint 11A.10 — Password Recovery. Consolidated into one migration by explicit
-- instruction (password_reset_tokens and auth_version are two halves of the same
-- mechanism) — see docs/design/DT-011A.10-password-recovery.md §14.

-- auth_version: per-user counter embedded in every access token as the "authVersion"
-- claim. ActiveAccountFilter compares it by equality against the current DB value on
-- every request, so incrementing it immediately invalidates every access token issued
-- before the increment — the mechanism a password reset uses to kill all existing
-- sessions instantly. DEFAULT 0 guarantees no existing session is invalidated just by
-- applying this migration (see DT §5/§21).
ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

-- password_reset_tokens: same "hash only, raw token shown once" pattern as
-- owner_invites (V12) / estimate_shares (V10). created_by is nullable: NULL for a
-- self-service POST /auth/password/forgot request, the acting SUPER_ADMIN's id for
-- the admin-triggered reset-link endpoint (DT §1/§15).
CREATE TABLE password_reset_tokens (
    id         UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id    UUID                     NOT NULL,
    token_hash VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_password_reset_tokens            PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user       FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_password_reset_tokens_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
