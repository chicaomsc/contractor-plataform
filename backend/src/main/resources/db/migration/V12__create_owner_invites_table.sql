-- Sprint 11A.7 — owner_invites: one-time-use, hashed, expiring invitation tokens used
-- by the platform admin onboarding flow (SUPER_ADMIN creates Company + PENDING OWNER,
-- then invites them to set their own password) and by reissuing/revoking a pending
-- invite. Same "hash only, raw token shown once" pattern as estimate_shares (V10) —
-- see docs/design/DT-011A.7-platform-admin-multitenancy.md §12.
CREATE TABLE owner_invites (
    id         UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id    UUID                     NOT NULL,
    token_hash VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_owner_invites            PRIMARY KEY (id),
    CONSTRAINT fk_owner_invites_user       FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_owner_invites_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_owner_invites_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_owner_invites_user_id ON owner_invites (user_id);
