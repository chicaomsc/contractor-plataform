-- DT-012 (Session Lifecycle, Phase A) — absolute session lifetime.
--
-- session_started_at records when the ORIGINAL login/register/invite-accept that
-- started this session happened — the application copies it forward unchanged onto
-- every refresh token a rotation issues (it is never recalculated on refresh). This
-- lets AuthService.refresh() reject a refresh once the session is older than
-- app.session.absolute-lifetime-seconds (default 8h), independent of each individual
-- refresh token's own TTL/expiry, and independent of how many times it has rotated.
--
-- Backward-compatible with rows already in production (refresh_tokens is a live table,
-- not a fresh one — Sprint DT-012 audit confirmed real rows exist today):
--   1. Add the column nullable first — never fails regardless of existing row count.
--   2. Backfill every existing row from its own created_at. This is the best available
--      approximation: for a token issued before this column existed, its own creation
--      is the closest known lower bound for when its session actually started (the
--      true original login time was never recorded before now). At worst this makes an
--      old, already-rotated session's absolute-lifetime clock start a little later than
--      the real original login — i.e. it may allow a few extra rotations for a
--      pre-existing session, never fewer. It can never make an old token expire too
--      early, and it never applies to any session created after this migration runs.
--   3. Only once every row is guaranteed to have a value does this ALTER the column to
--      NOT NULL, matching every other token table in this schema (owner_invites,
--      password_reset_tokens all have fully-populated, non-nullable columns).
--
-- No data is deleted or destructively rewritten — every existing refresh_tokens row
-- keeps its id, token_hash, expires_at, and revoked/created_at/updated_at exactly as-is;
-- only the new column is populated.
ALTER TABLE refresh_tokens ADD COLUMN session_started_at TIMESTAMP WITH TIME ZONE;

UPDATE refresh_tokens SET session_started_at = created_at WHERE session_started_at IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN session_started_at SET NOT NULL;
