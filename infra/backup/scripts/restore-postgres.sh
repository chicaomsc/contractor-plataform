#!/usr/bin/env bash
# Contractor Platform — PostgreSQL restore (DT-011A.5 §18). Deliberately
# conservative: without --confirm this only resolves and validates a snapshot
# (read-only), never touches the database.
#
# "Clean database" here means `pg_restore --clean --if-exists` inside the
# EXISTING database/role (drops conflicting objects before recreating them),
# not a DROP DATABASE/CREATE DATABASE cycle — the latter needs a separate
# maintenance-database connection and a "terminate all other connections"
# step, which is more failure-prone to automate than it's worth for a
# single-schema, single-tenant-role database. An operator who wants a fully
# fresh database can drop/recreate it by hand before running this script.
#
# IMPORTANT (explicit, per DT-011A.5 §28): this restores DATA, not CODE. A
# restored dump may reflect an OLDER schema than the currently-deployed
# backend image. Flyway does not downgrade. If the currently-running
# APP_VERSION expects migrations that this dump doesn't have, choose a
# compatible APP_VERSION (docs/design/DT-011A.4-ghcr-image-pipeline.md)
# before starting the backend against a restored database.

set -Eeuo pipefail

# shellcheck disable=SC2034 # used by log() in lib.sh, not visible per-file
SCRIPT_NAME="restore-postgres"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./lib.sh
source "${SCRIPT_DIR}/lib.sh"

umask 077

usage() {
  cat >&2 <<'EOF'
Usage: restore-postgres.sh <snapshot-id|latest> [--confirm]

Without --confirm: resolves the snapshot, retrieves and validates the dump,
prints what WOULD happen, and exits 2 — no database is touched.

With --confirm: stops the `backend` service, restores the dump into the
existing database with `pg_restore --clean --if-exists`, and prints the
next steps (it does NOT restart `backend` for you).
EOF
}

main() {
  [ $# -ge 1 ] || { usage; die "missing <snapshot-id|latest>"; }
  local snapshot="$1"
  local confirmed=0
  shift
  while [ $# -gt 0 ]; do
    case "$1" in
      --confirm) confirmed=1; shift ;;
      -h|--help) usage; exit 0 ;;
      *) usage; die "unknown argument: $1" ;;
    esac
  done

  load_backup_env
  require_var BACKUP_COMPOSE_FILE
  require_var BACKUP_PRODUCTION_ENV
  require_var BACKUP_STAGING_DIR
  require_cmd docker

  require_file "$BACKUP_PRODUCTION_ENV" "BACKUP_PRODUCTION_ENV"
  set -a
  # shellcheck source=/dev/null
  source "$BACKUP_PRODUCTION_ENV"
  set +a
  require_var POSTGRES_DB
  require_var POSTGRES_USER
  require_var POSTGRES_PASSWORD

  log "resolving snapshot '${snapshot}' (tag=postgres, host=contractor-platform)"
  local file_path
  file_path="$(restic_run -- ls "$snapshot" --tag postgres --host contractor-platform /staging \
    | grep -E '\.dump$' | head -n1 || true)"
  [ -n "$file_path" ] || die "no .dump file found under /staging in snapshot '${snapshot}'"
  log "found dump in snapshot: ${file_path}"

  local staging_dir restore_path
  staging_dir="${BACKUP_STAGING_DIR%/}/postgres-restore"
  restore_path="${staging_dir}/restore.dump"
  mkdir -p "$staging_dir" && chmod 700 "$staging_dir"
  cleanup_on_exit "$staging_dir"

  log "retrieving dump content"
  restic_run -- dump "$snapshot" "$file_path" --tag postgres --host contractor-platform > "$restore_path"

  [ -s "$restore_path" ] || die "retrieved dump is empty — refusing to restore"

  log "validating dump catalog (pg_restore --list)"
  if ! docker run --rm -v "${restore_path}:/dump.dump:ro" postgres:17-alpine \
      pg_restore --list /dump.dump >/dev/null; then
    die "retrieved dump failed pg_restore --list validation — refusing to restore"
  fi
  log "dump validated OK"

  if [ "$confirmed" -ne 1 ]; then
    log "DRY RUN — no database was touched. Re-run with --confirm to restore into '${POSTGRES_DB}'."
    exit 2
  fi

  local pg_cid
  pg_cid="$(compose_container_id postgres)"

  log "stopping backend to avoid writes during restore"
  docker compose -f "$BACKUP_COMPOSE_FILE" --env-file "$BACKUP_PRODUCTION_ENV" stop backend

  log "restoring into database '${POSTGRES_DB}' (pg_restore --clean --if-exists)"
  if ! docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" "$pg_cid" \
      pg_restore --clean --if-exists --no-owner \
        --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" \
      < "$restore_path"; then
    log "WARNING: pg_restore reported a non-zero exit — this can be harmless (e.g. DROP ... IF EXISTS notices) or can mean a real problem. Review the output above before trusting this restore."
  fi

  cat >&2 <<EOF

Restore attempt finished. Next steps (not automated further):
  1. Start the backend: docker compose -f ${BACKUP_COMPOSE_FILE} --env-file ${BACKUP_PRODUCTION_ENV} up -d backend
  2. Check its logs for Flyway: "Schema is up to date" or a validated migration count.
  3. IMPORTANT: this dump may be OLDER than the schema the current APP_VERSION expects.
     If Flyway or the app fails to start, choose an APP_VERSION whose migrations match
     this dump (see docs/design/DT-011A.4-ghcr-image-pipeline.md) before retrying.
  4. Smoke test: log in, confirm expected data is present.
EOF
}

main "$@"
