-- Sprint 5: Company Admin — add profile fields to existing tables
-- Only column additions; no new tables.

-- companies: trade name, contact extras, tax number
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS trade_name  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS whatsapp    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS website     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tax_number  VARCHAR(50);

-- brandings: quotation / document identity fields
ALTER TABLE brandings
    ADD COLUMN IF NOT EXISTS footer_text       TEXT,
    ADD COLUMN IF NOT EXISTS quotation_prefix  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS signature_name    VARCHAR(255);

-- settings: output formatting preferences
ALTER TABLE settings
    ADD COLUMN IF NOT EXISTS date_format    VARCHAR(50) NOT NULL DEFAULT 'dd/MM/yyyy',
    ADD COLUMN IF NOT EXISTS number_format  VARCHAR(50) NOT NULL DEFAULT 'pt-PT';
