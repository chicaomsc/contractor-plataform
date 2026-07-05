-- ─────────────────────────────────────────────────────────────────────────────
-- Sprint 3 — Company core tables
-- All domain entities carry company_id for multi-tenant isolation.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE companies (
    id                  UUID                        NOT NULL DEFAULT uuid_generate_v4(),
    name                VARCHAR(255)                NOT NULL,
    slug                VARCHAR(100)                NOT NULL,
    email               VARCHAR(255)                NOT NULL,
    phone               VARCHAR(50),
    country             VARCHAR(2)                  NOT NULL,

    -- Address (embedded value object)
    address_street      VARCHAR(255),
    address_city        VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_region      VARCHAR(100),
    address_country     VARCHAR(2),

    status              VARCHAR(20)                 NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE    NOT NULL,

    CONSTRAINT pk_companies      PRIMARY KEY (id),
    CONSTRAINT uq_companies_slug UNIQUE (slug)
);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE brandings (
    id              UUID                        NOT NULL DEFAULT uuid_generate_v4(),
    company_id      UUID                        NOT NULL,
    logo_url        TEXT,
    primary_color   VARCHAR(7),
    secondary_color VARCHAR(7),
    tagline         VARCHAR(500),
    about_text      TEXT,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL,

    CONSTRAINT pk_brandings         PRIMARY KEY (id),
    CONSTRAINT fk_brandings_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_brandings_company UNIQUE (company_id)
);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE settings (
    id                      UUID                        NOT NULL DEFAULT uuid_generate_v4(),
    company_id              UUID                        NOT NULL,
    default_currency        VARCHAR(3)                  NOT NULL DEFAULT 'EUR',
    default_tax_rate        NUMERIC(5, 2)               NOT NULL DEFAULT 0.00,
    estimate_validity_days  INTEGER                     NOT NULL DEFAULT 30,
    estimate_footer_text    TEXT,
    created_at              TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE    NOT NULL,

    CONSTRAINT pk_settings         PRIMARY KEY (id),
    CONSTRAINT fk_settings_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_settings_company UNIQUE (company_id)
);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id            UUID                        NOT NULL DEFAULT uuid_generate_v4(),
    company_id    UUID                        NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    role          VARCHAR(20)                 NOT NULL DEFAULT 'OWNER',
    status        VARCHAR(20)                 NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE    NOT NULL,

    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_users_email   UNIQUE (email)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Indices for common query patterns
-- ─────────────────────────────────────────────────────────────────────────────

CREATE INDEX idx_brandings_company_id ON brandings (company_id);
CREATE INDEX idx_settings_company_id  ON settings  (company_id);
CREATE INDEX idx_users_company_id     ON users     (company_id);
CREATE INDEX idx_users_email          ON users     (email);
