-- Sprint 6: Service Catalogue and Portfolio Gallery

-- ── services ──────────────────────────────────────────────────────────────────
CREATE TABLE services (
    id                UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    company_id        UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    slug              VARCHAR(255)             NOT NULL,
    short_description VARCHAR(500),
    description       TEXT,
    icon              VARCHAR(100),
    display_order     INTEGER                  NOT NULL DEFAULT 0,
    active            BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_services             PRIMARY KEY (id),
    CONSTRAINT fk_services_company     FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_services_company_slug UNIQUE (company_id, slug)
);

CREATE INDEX idx_services_company_id    ON services (company_id);
CREATE INDEX idx_services_display_order ON services (company_id, display_order);
CREATE INDEX idx_services_active        ON services (company_id, active);

-- ── gallery_items ─────────────────────────────────────────────────────────────
CREATE TABLE gallery_items (
    id               UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    company_id       UUID                     NOT NULL,
    title            VARCHAR(255)             NOT NULL,
    description      TEXT,
    before_image_url VARCHAR(2048),
    after_image_url  VARCHAR(2048),
    display_order    INTEGER                  NOT NULL DEFAULT 0,
    featured         BOOLEAN                  NOT NULL DEFAULT FALSE,
    active           BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_gallery_items         PRIMARY KEY (id),
    CONSTRAINT fk_gallery_items_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE INDEX idx_gallery_items_company_id    ON gallery_items (company_id);
CREATE INDEX idx_gallery_items_display_order ON gallery_items (company_id, display_order);
CREATE INDEX idx_gallery_items_featured      ON gallery_items (company_id, featured);
CREATE INDEX idx_gallery_items_active        ON gallery_items (company_id, active);
