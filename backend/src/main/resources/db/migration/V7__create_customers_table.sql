-- Sprint 10A: Estimate domain — Customer portfolio

CREATE TABLE customers (
    id                  UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    company_id          UUID                     NOT NULL,
    name                VARCHAR(255)             NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(50),
    tax_number          VARCHAR(50),

    -- Address (embedded value object, same shape as companies.*)
    address_street      VARCHAR(255),
    address_city        VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_region      VARCHAR(100),
    address_country     VARCHAR(2),

    notes               TEXT,
    active              BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_customers         PRIMARY KEY (id),
    CONSTRAINT fk_customers_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE INDEX idx_customers_company_id ON customers (company_id);
CREATE INDEX idx_customers_active     ON customers (company_id, active);
