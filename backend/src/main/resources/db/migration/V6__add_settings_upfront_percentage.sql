-- Sprint 10A: Estimate domain — default upfront percentage snapshot source
-- Estimates snapshot this value at creation time; changing it later never
-- affects estimates already created (see ADR-007).

ALTER TABLE settings
    ADD COLUMN IF NOT EXISTS upfront_percentage NUMERIC(5, 2) NOT NULL DEFAULT 50.00;
