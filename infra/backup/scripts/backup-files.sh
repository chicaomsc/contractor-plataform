#!/usr/bin/env bash
# Contractor Platform — filesystem backup: backend_storage (required) and
# caddy_data (recommended/operational, not business-critical) — DT-011A.5 §9/§10.
#
# Restic reads each volume through `--volumes-from <container>:ro` on the
# already-running service containers — no extra copy, no guessing the
# Compose-project-prefixed volume name, and the `:ro` suffix makes it
# structurally impossible for the backup process to write into either volume.
# The backend/caddy containers are never stopped or paused: this is a
# deliberately crash-consistent backup (see DT-011A.5 §5.4/§9.2 — uploads use a
# unique filename per file and the DB record for one is only written after the
# file finishes, so the realistic worst case is a single in-flight upload
# appearing incomplete in one snapshot, not a corrupted existing file).
#
# caddy_config is intentionally NOT backed up here — DT-011A.5 §10: it is
# derived entirely from the versioned infra/caddy/Caddyfile.

set -Eeuo pipefail

# shellcheck disable=SC2034 # used by log() in lib.sh, not visible per-file
SCRIPT_NAME="backup-files"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./lib.sh
source "${SCRIPT_DIR}/lib.sh"

usage() {
  cat >&2 <<'EOF'
Usage: backup-files.sh [--target backend-storage|caddy-data|all]

Defaults to --target all (both backend_storage and caddy_data).
EOF
}

backup_backend_storage() {
  local backend_cid
  backend_cid="$(compose_container_id backend)"
  log "backend container: ${backend_cid} (backend_storage, read-only mount)"
  restic_run --volumes-from "${backend_cid}:ro" -- \
    backup /app/storage \
    --tag backend-storage --tag "type=backend-storage" \
    --host contractor-platform
  restic_run -- snapshots --tag backend-storage --latest 1
}

backup_caddy_data() {
  local caddy_cid
  caddy_cid="$(compose_container_id caddy)"
  log "caddy container: ${caddy_cid} (caddy_data, read-only mount)"
  restic_run --volumes-from "${caddy_cid}:ro" -- \
    backup /data \
    --tag caddy-data --tag "type=caddy-data" \
    --host contractor-platform
  restic_run -- snapshots --tag caddy-data --latest 1
}

main() {
  local target="all"
  while [ $# -gt 0 ]; do
    case "$1" in
      --target)
        target="${2:-}"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        usage
        die "unknown argument: $1"
        ;;
    esac
  done

  case "$target" in
    backend-storage|caddy-data|all) ;;
    *) usage; die "invalid --target: ${target}" ;;
  esac

  load_backup_env
  require_var BACKUP_COMPOSE_FILE
  require_var BACKUP_PRODUCTION_ENV
  require_cmd docker

  if [ "$target" = "backend-storage" ] || [ "$target" = "all" ]; then
    backup_backend_storage
  fi
  if [ "$target" = "caddy-data" ] || [ "$target" = "all" ]; then
    backup_caddy_data
  fi

  log "files backup complete (target=${target})"
}

main "$@"
