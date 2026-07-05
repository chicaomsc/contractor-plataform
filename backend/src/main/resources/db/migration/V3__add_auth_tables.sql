-- ─────────────────────────────────────────────────────────────────────────────
-- Sprint 4 — Auth & onboarding additions
-- ─────────────────────────────────────────────────────────────────────────────

-- Branding: accent colour (nullable — clients may not set it)
ALTER TABLE brandings ADD COLUMN accent_color VARCHAR(7);

-- Settings: locale and timezone for i18n / PDF generation
ALTER TABLE settings ADD COLUMN locale   VARCHAR(10)  NOT NULL DEFAULT 'pt-PT';
ALTER TABLE settings ADD COLUMN timezone VARCHAR(50)  NOT NULL DEFAULT 'Europe/Lisbon';

-- ─────────────────────────────────────────────────────────────────────────────
-- Refresh tokens — opaque random strings stored server-side
-- (enables immediate revocation and rotation)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id    UUID                     NOT NULL,
    token      VARCHAR(512)             NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_refresh_tokens       PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user  FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens (token);
