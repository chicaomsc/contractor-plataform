-- Sprint 10A: Estimate domain — Estimate, EstimateItem, Material, and the
-- per-company/year sequence backing the human-readable estimate number (see ADR-007).

-- ── estimate_number_sequences ────────────────────────────────────────────────
-- One row per (company, year). Advanced atomically via INSERT ... ON CONFLICT DO
-- UPDATE ... RETURNING in EstimateNumberGenerator — never read-then-write from the app.
CREATE TABLE estimate_number_sequences (
    company_id UUID    NOT NULL,
    year       INTEGER NOT NULL,
    last_value INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_estimate_number_sequences         PRIMARY KEY (company_id, year),
    CONSTRAINT fk_estimate_number_sequences_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

-- ── estimates ─────────────────────────────────────────────────────────────────
CREATE TABLE estimates (
    id                       UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    company_id               UUID                     NOT NULL,
    customer_id              UUID                     NOT NULL,
    number                   VARCHAR(50)              NOT NULL,
    title                    VARCHAR(255)             NOT NULL,
    description              TEXT,
    status                   VARCHAR(20)              NOT NULL DEFAULT 'DRAFT',
    issue_date               DATE                     NOT NULL,
    valid_until              DATE,
    expected_start_date      DATE,
    estimated_duration_days  INTEGER,
    notes                    TEXT,
    terms                    TEXT,

    -- Snapshots — never re-read from settings/branding after creation
    currency                 VARCHAR(3)               NOT NULL,
    vat_rate                 NUMERIC(5, 2)            NOT NULL,
    upfront_percentage       NUMERIC(5, 2)            NOT NULL,

    -- Backend-calculated totals
    labor_subtotal           NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    material_subtotal        NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    subtotal                 NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    vat_amount                NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    total                    NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    upfront_amount           NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    remaining_amount         NUMERIC(12, 2)           NOT NULL DEFAULT 0,

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_estimates             PRIMARY KEY (id),
    CONSTRAINT fk_estimates_company     FOREIGN KEY (company_id)  REFERENCES companies (id),
    CONSTRAINT fk_estimates_customer    FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uq_estimates_company_number UNIQUE (company_id, number),
    CONSTRAINT ck_estimates_vat_rate    CHECK (vat_rate >= 0),
    CONSTRAINT ck_estimates_upfront_pct CHECK (upfront_percentage >= 0 AND upfront_percentage <= 100),
    CONSTRAINT ck_estimates_totals_non_negative CHECK (
        labor_subtotal >= 0 AND material_subtotal >= 0 AND subtotal >= 0 AND
        vat_amount >= 0 AND total >= 0 AND upfront_amount >= 0 AND remaining_amount >= 0
    )
);

CREATE INDEX idx_estimates_company_id  ON estimates (company_id);
CREATE INDEX idx_estimates_status      ON estimates (company_id, status);
CREATE INDEX idx_estimates_customer_id ON estimates (company_id, customer_id);

-- ── estimate_items ───────────────────────────────────────────────────────────
CREATE TABLE estimate_items (
    id             UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    estimate_id    UUID                     NOT NULL,
    service_id     UUID,
    description    TEXT                     NOT NULL,
    quantity       NUMERIC(12, 3)           NOT NULL,
    unit           VARCHAR(20)              NOT NULL,
    unit_price     NUMERIC(12, 2)           NOT NULL,
    total          NUMERIC(12, 2)           NOT NULL,
    display_order  INTEGER                  NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    -- No FK to services(id) on purpose: it is a soft, point-in-time catalogue reference.
    -- Deleting or editing a catalogue service must never affect existing estimates.
    CONSTRAINT pk_estimate_items          PRIMARY KEY (id),
    CONSTRAINT fk_estimate_items_estimate FOREIGN KEY (estimate_id) REFERENCES estimates (id) ON DELETE CASCADE,
    CONSTRAINT ck_estimate_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_estimate_items_price    CHECK (unit_price >= 0)
);

CREATE INDEX idx_estimate_items_estimate_id ON estimate_items (estimate_id);

-- ── materials ─────────────────────────────────────────────────────────────────
-- Belongs directly to Estimate (not to EstimateItem) — no global material catalogue in this sprint.
CREATE TABLE materials (
    id             UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    estimate_id    UUID                     NOT NULL,
    name           VARCHAR(255)             NOT NULL,
    description    TEXT,
    quantity       NUMERIC(12, 3)           NOT NULL,
    unit           VARCHAR(20)              NOT NULL,
    unit_price     NUMERIC(12, 2)           NOT NULL,
    total          NUMERIC(12, 2)           NOT NULL,
    display_order  INTEGER                  NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_materials          PRIMARY KEY (id),
    CONSTRAINT fk_materials_estimate FOREIGN KEY (estimate_id) REFERENCES estimates (id) ON DELETE CASCADE,
    CONSTRAINT ck_materials_quantity CHECK (quantity > 0),
    CONSTRAINT ck_materials_price    CHECK (unit_price >= 0)
);

CREATE INDEX idx_materials_estimate_id ON materials (estimate_id);
