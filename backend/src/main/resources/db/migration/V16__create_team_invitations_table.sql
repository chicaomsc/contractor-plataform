-- DT-017B — Team Invitations Backend.
--
-- Deliberately a SEPARATE table from owner_invites, not a reuse of it — the two model
-- different provisioning flows (SUPER_ADMIN -> owner_invites -> OWNER, always exactly
-- one new Company; OWNER -> team_invitations -> MANAGER|MEMBER, an existing Company
-- gaining a collaborator) and, unlike owner_invites, this table carries its own email
-- and role directly: a team invitation is issued BEFORE any User row exists for that
-- person (owner_invites instead always points at an already-created PENDING User —
-- see docs/design/DT-017B-team-invitations-backend.md for the full rationale).
--
-- Same "hash only, shown once" pattern as owner_invites/password_reset_tokens/
-- refresh_tokens — token_hash is the only form of the token ever persisted.
CREATE TABLE team_invitations (
    id                 UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    company_id         UUID                     NOT NULL,
    email              VARCHAR(255)             NOT NULL,
    role               VARCHAR(20)              NOT NULL,
    token_hash         VARCHAR(64)              NOT NULL,
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at            TIMESTAMP WITH TIME ZONE,
    revoked_at         TIMESTAMP WITH TIME ZONE,
    invited_by_user_id UUID                     NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_team_invitations           PRIMARY KEY (id),
    CONSTRAINT fk_team_invitations_company   FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_team_invitations_invited_by FOREIGN KEY (invited_by_user_id) REFERENCES users (id),
    CONSTRAINT uq_team_invitations_token_hash UNIQUE (token_hash),
    -- Defense in depth alongside TeamInvitationService's own role check (DT-017A
    -- introduced MANAGER/MEMBER; this table must never carry OWNER or SUPER_ADMIN —
    -- a team invitation can never provision either) — same two-layer pattern as
    -- chk_users_role_company_id (V11): the CHECK constraint is the real,
    -- non-bypassable guarantee, the application-level check is only a friendlier,
    -- earlier error message.
    CONSTRAINT chk_team_invitations_role      CHECK (role IN ('MANAGER', 'MEMBER'))
);

-- uq_team_invitations_token_hash above already creates an index for token_hash
-- lookups (POST /auth/team-invitations/accept) — no separate index needed for it,
-- same as owner_invites/password_reset_tokens/refresh_tokens.
CREATE INDEX idx_team_invitations_company_id ON team_invitations (company_id);
CREATE INDEX idx_team_invitations_email      ON team_invitations (email);
