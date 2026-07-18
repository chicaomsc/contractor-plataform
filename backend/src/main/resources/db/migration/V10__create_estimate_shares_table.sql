-- Sprint 10D: Estimate Sharing — a public, unauthenticated, revocable/expiring link
-- to view/download a single estimate. Only the token hash is stored (SHA-256): the raw
-- token is returned to the owner once, at creation time, and never persisted or shown
-- again — the same "shown once" model as an API key.
--
-- ON DELETE CASCADE from estimates: an estimate can only be hard-deleted while DRAFT
-- (see EstimateService.deleteEstimate); when that happens, any share row for it must
-- stop resolving. Cascading at the DB level guarantees this without extra application code.
CREATE TABLE estimate_shares (
    id                 UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    company_id         UUID                     NOT NULL,
    estimate_id        UUID                     NOT NULL,
    token_hash         VARCHAR(64)              NOT NULL,
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at         TIMESTAMP WITH TIME ZONE,
    last_access_at     TIMESTAMP WITH TIME ZONE,
    access_count       BIGINT                   NOT NULL DEFAULT 0,
    created_by_user_id UUID                     NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_estimate_shares          PRIMARY KEY (id),
    CONSTRAINT fk_estimate_shares_company  FOREIGN KEY (company_id)  REFERENCES companies (id),
    CONSTRAINT fk_estimate_shares_estimate FOREIGN KEY (estimate_id) REFERENCES estimates (id) ON DELETE CASCADE,
    CONSTRAINT uq_estimate_shares_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_estimate_shares_access_count CHECK (access_count >= 0)
);

CREATE INDEX idx_estimate_shares_estimate_id ON estimate_shares (estimate_id, company_id);
CREATE INDEX idx_estimate_shares_company_id  ON estimate_shares (company_id);
