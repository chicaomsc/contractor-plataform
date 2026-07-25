#!/usr/bin/env bash
# Contractor Platform — orchestrates the full daily backup (DT-011A.5 §15/§16):
# PostgreSQL, backend_storage, caddy_data, then retention. This is what
# contractor-platform-backup.service calls; it is also exactly what an operator
# runs by hand.
#
# Retention (`restic forget --prune`) only runs if EVERY backup step above
# succeeded. A failed critical backup must never be followed by a prune that
# behaves as if the run were healthy — see DT-011A.5 "backup-all.sh" scope.

set -Eeuo pipefail

# shellcheck disable=SC2034 # used by log() in lib.sh, not visible per-file
SCRIPT_NAME="backup-all"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./lib.sh
source "${SCRIPT_DIR}/lib.sh"

usage() {
  cat >&2 <<'EOF'
Usage: backup-all.sh [--skip-retention] [--with-check]

  --skip-retention  Run the backups but skip `restic forget --prune`.
  --with-check      Also run a lightweight `restic check` (no --read-data)
                     after a successful retention pass. Full-data checks are
                     a separate, periodic operation — see
                     infra/backup/README.md "Verificação de integridade".
EOF
}

run_retention() {
  require_var BACKUP_RETENTION_DAILY
  require_var BACKUP_RETENTION_WEEKLY
  require_var BACKUP_RETENTION_MONTHLY

  log "applying retention (daily=${BACKUP_RETENTION_DAILY} weekly=${BACKUP_RETENTION_WEEKLY} monthly=${BACKUP_RETENTION_MONTHLY})"
  # --group-by tags: each distinct tag combination (postgres / backend-storage /
  # caddy-data) gets its own independent daily/weekly/monthly count instead of
  # being pruned together as one pool.
  restic_run -- forget \
    --keep-daily "$BACKUP_RETENTION_DAILY" \
    --keep-weekly "$BACKUP_RETENTION_WEEKLY" \
    --keep-monthly "$BACKUP_RETENTION_MONTHLY" \
    --group-by tags \
    --prune
}

main() {
  local skip_retention=0 with_check=0
  while [ $# -gt 0 ]; do
    case "$1" in
      --skip-retention) skip_retention=1; shift ;;
      --with-check) with_check=1; shift ;;
      -h|--help) usage; exit 0 ;;
      *) usage; die "unknown argument: $1" ;;
    esac
  done

  load_backup_env

  log "=== starting full backup run ==="

  if ! "${SCRIPT_DIR}/backup-postgres.sh"; then
    die "postgres backup failed — aborting before files/retention (no prune will run)"
  fi

  if ! "${SCRIPT_DIR}/backup-files.sh" --target all; then
    die "files backup (backend_storage/caddy_data) failed — aborting before retention (no prune will run)"
  fi

  if [ "$skip_retention" -eq 1 ]; then
    log "skipping retention (--skip-retention)"
  else
    run_retention
  fi

  if [ "$with_check" -eq 1 ]; then
    log "running restic check (no --read-data — see README for full data checks)"
    restic_run -- check
  fi

  log "=== full backup run complete ==="
}

main "$@"
