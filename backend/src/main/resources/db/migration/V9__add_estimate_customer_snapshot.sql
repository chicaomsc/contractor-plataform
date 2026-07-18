-- Sprint 10C: Professional Estimate PDF — Customer snapshot on Estimate
--
-- Estimate.customerId was a pure FK (Sprint 10A risk, documented in
-- docs/releases/v1.0.0-estimate-domain-api.md). Editing a Customer after an Estimate was
-- created must never change what a previously-generated document showed. This migration
-- freezes the customer's identity/contact data at the moment it is assigned to an Estimate
-- (on creation, or when the assigned customer changes on a PUT). It is NOT a general
-- versioning system — only the fields rendered on the PDF are captured.

ALTER TABLE estimates
    ADD COLUMN customer_name_snapshot                  VARCHAR(255),
    ADD COLUMN customer_email_snapshot                  VARCHAR(255),
    ADD COLUMN customer_phone_snapshot                  VARCHAR(50),
    ADD COLUMN customer_tax_number_snapshot             VARCHAR(50),
    ADD COLUMN customer_address_street_snapshot         VARCHAR(255),
    ADD COLUMN customer_address_city_snapshot           VARCHAR(100),
    ADD COLUMN customer_address_postal_code_snapshot    VARCHAR(20),
    ADD COLUMN customer_address_region_snapshot         VARCHAR(100),
    ADD COLUMN customer_address_country_snapshot        VARCHAR(2);

-- Backfill: the only data available at migration time is the customer's CURRENT record.
-- From this point forward, EstimateService freezes the snapshot at creation/reassignment
-- time, so this is a one-time, best-effort reconciliation for pre-existing estimates.
UPDATE estimates e
SET customer_name_snapshot               = c.name,
    customer_email_snapshot              = c.email,
    customer_phone_snapshot              = c.phone,
    customer_tax_number_snapshot         = c.tax_number,
    customer_address_street_snapshot     = c.address_street,
    customer_address_city_snapshot       = c.address_city,
    customer_address_postal_code_snapshot = c.address_postal_code,
    customer_address_region_snapshot     = c.address_region,
    customer_address_country_snapshot    = c.address_country
FROM customers c
WHERE c.id = e.customer_id;

-- customers.name is NOT NULL, so the backfill above guarantees every existing estimate now
-- has a non-null name snapshot — safe to enforce going forward. The other fields mirror
-- Customer's own nullable columns and stay nullable here.
ALTER TABLE estimates
    ALTER COLUMN customer_name_snapshot SET NOT NULL;
