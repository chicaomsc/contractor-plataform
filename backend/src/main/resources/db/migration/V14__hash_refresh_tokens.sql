-- Sprint 11B.6A — Authentication Hardening (SEC-AUTH-01, DT-011B.5 §9 HARD-03).
--
-- refresh_tokens.token was stored in plain text — the only token in the system not
-- hashed at rest, unlike owner_invites/password_reset_tokens (SHA-256, hash-only).
-- No production data exists yet (pre-Sprint 11C), so this is a hard cutover: drop the
-- plaintext column, add a hash-only column following the exact same shape already
-- used by owner_invites.token_hash/password_reset_tokens.token_hash.
-- Existing rows (plaintext tokens) can't be backfilled into a hash column — discarding
-- them just forces an unnoticed re-login on next refresh, no real data loss.
DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens DROP CONSTRAINT uq_refresh_tokens_token;
DROP INDEX idx_refresh_tokens_token;
ALTER TABLE refresh_tokens DROP COLUMN token;

ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64) NOT NULL;
ALTER TABLE refresh_tokens ADD CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
