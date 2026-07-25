#!/usr/bin/env bash
# Contractor Platform — PostgreSQL backup (DT-011A.5 §8).
#
# pg_dump --format=custom, run inside the already-running `postgres` container
# (same binary version as the server, no client tooling installed on the host),
# staged briefly on local disk, validated, then handed to Restic. Never
# considers the backup successful if pg_dump failed.
#
# Why staging, not streaming (`pg_dump | restic backup --stdin`): with
# `set -o pipefail` a failed pg_dump WOULD still make this script exit non-zero
# even in a streaming pipe — but `restic backup --stdin` will have already
# stored whatever partial bytes arrived before pg_dump died, creating a
# snapshot that *looks* like a normal daily backup and is actually corrupt.
# Staging validates the dump (size, `pg_restore --list`) BEFORE anything reaches
# Restic, so a failed pg_dump never produces a snapshot at all. The staging
# file is root of a single small `trap`-cleaned directory (mode 700, umask 077)
# and is removed on every exit path — see cleanup_on_exit in lib.sh.

set -Eeuo pipefail

# shellcheck disable=SC2034 # used by log() in lib.sh, not visible per-file
SCRIPT_NAME="backup-postgres"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./lib.sh
source "${SCRIPT_DIR}/lib.sh"

umask 077

main() {
  load_backup_env
  require_var BACKUP_COMPOSE_FILE
  require_var BACKUP_PRODUCTION_ENV
  require_var BACKUP_STAGING_DIR
  require_cmd docker

  require_file "$BACKUP_PRODUCTION_ENV" "BACKUP_PRODUCTION_ENV"
  # production.env is the same plain KEY=VALUE shape as backup.env (see
  # infra/env/production.env.example) — read here only to learn
  # POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD, never logged or forwarded
  # anywhere except as PGPASSWORD for the single `docker exec` below.
  set -a
  # shellcheck source=/dev/null
  source "$BACKUP_PRODUCTION_ENV"
  set +a
  require_var POSTGRES_DB
  require_var POSTGRES_USER
  require_var POSTGRES_PASSWORD

  local pg_cid
  pg_cid="$(compose_container_id postgres)"
  log "postgres container: ${pg_cid}"

  if ! docker exec "$pg_cid" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
    die "postgres is not ready (pg_isready failed) — refusing to attempt a dump"
  fi

  local staging_dir ts dump_name dump_path
  ts="$(date -u +%Y%m%dT%H%M%SZ)"
  dump_name="${POSTGRES_DB}_${ts}.dump"
  staging_dir="${BACKUP_STAGING_DIR%/}/postgres"
  dump_path="${staging_dir}/${dump_name}"

  mkdir -p "$staging_dir" && chmod 700 "$staging_dir"
  cleanup_on_exit "$staging_dir"

  log "running pg_dump --format=custom for database '${POSTGRES_DB}'"
  # PGPASSWORD is exported only into this one `docker exec` invocation's
  # environment — never a CLI argument, never echoed (no `set -x` anywhere
  # near this). pg_dump writes to its own stdout (no --file), which `docker
  # exec` streams straight to the host file below; no docker cp round trip,
  # no dump file ever created inside the postgres container itself.
  if ! docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$pg_cid" \
      pg_dump --format=custom --compress=6 \
        --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" \
      > "$dump_path"; then
    die "pg_dump failed — no snapshot will be created"
  fi

  [ -s "$dump_path" ] || die "pg_dump produced an empty file — refusing to back it up"

  log "validating dump catalog (pg_restore --list)"
  if ! docker run --rm -v "${dump_path}:/dump.dump:ro" postgres:17-alpine \
      pg_restore --list /dump.dump >/dev/null; then
    die "dump failed pg_restore --list validation — refusing to back it up"
  fi

  # production.env travels in the same snapshot (DT-011A.5 §11) — it's the only
  # way to reconstruct VPS config/secrets after losing the host entirely, and
  # it's protected by the exact same Restic encryption as the dump next to it.
  # Copied, never moved: infra/env/production.env itself is untouched.
  cp "$BACKUP_PRODUCTION_ENV" "${staging_dir}/production.env"
  chmod 600 "${staging_dir}/production.env"

  send_staging_to_restic "$staging_dir"

  log "confirming a new snapshot was created"
  restic_run -- snapshots --tag postgres --latest 1

  log "postgres backup complete: ${dump_name} (+ production.env)"
}

main "$@"
