#!/usr/bin/env bash
# Contractor Platform — filesystem restore for backend_storage or caddy_data
# (DT-011A.5 §19). Never writes directly into the real volume: every restore
# goes snapshot -> local temp directory -> validation -> controlled copy-in,
# and the copy-in step only runs with --confirm.
#
# Ownership: backend_storage files must belong to the backend container's
# fixed non-root user (uid=1000/gid=1000 — inspected in backend/Dockerfile:
# `addgroup -g 1000 platform && adduser -u 1000 ... platform`, not guessed).
# caddy_data needs no chown: the official caddy:2-alpine image runs as root
# (confirmed with `docker run --entrypoint sh caddy:2-alpine -c id`), matching
# how files land when copied in by this script's own (root) utility container.

set -Eeuo pipefail

# shellcheck disable=SC2034 # used by log() in lib.sh, not visible per-file
SCRIPT_NAME="restore-files"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./lib.sh
source "${SCRIPT_DIR}/lib.sh"

umask 077

usage() {
  cat >&2 <<'EOF'
Usage: restore-files.sh --target backend-storage|caddy-data <snapshot-id|latest>
                         [--path <path-relative-to-volume-root>] [--confirm]

Without --confirm: restores the snapshot (or just --path, if given) into a
local temporary directory, validates it, and exits 2 without touching the
real volume.

With --confirm: copies the validated content into the real volume through a
throwaway container (--volumes-from, writable) and, for backend-storage,
fixes ownership to uid:gid 1000:1000.

Examples:
  restore-files.sh --target backend-storage latest
  restore-files.sh --target backend-storage latest \
      --path company/<id>/logo/<file>.png --confirm
  restore-files.sh --target caddy-data latest --confirm
EOF
}

main() {
  local target="" snapshot="" rel_path="" confirmed=0

  while [ $# -gt 0 ]; do
    case "$1" in
      --target) target="${2:-}"; shift 2 ;;
      --path) rel_path="${2:-}"; shift 2 ;;
      --confirm) confirmed=1; shift ;;
      -h|--help) usage; exit 0 ;;
      *)
        if [ -z "$snapshot" ]; then
          snapshot="$1"
          shift
        else
          usage
          die "unexpected argument: $1"
        fi
        ;;
    esac
  done

  case "$target" in
    backend-storage|caddy-data) ;;
    *) usage; die "--target must be backend-storage or caddy-data" ;;
  esac
  [ -n "$snapshot" ] || { usage; die "missing <snapshot-id|latest>"; }

  load_backup_env
  require_var BACKUP_COMPOSE_FILE
  require_var BACKUP_PRODUCTION_ENV
  require_var BACKUP_STAGING_DIR
  require_cmd docker

  local tag volume_root service
  if [ "$target" = "backend-storage" ]; then
    tag="backend-storage"
    volume_root="/app/storage"
    service="backend"
  else
    tag="caddy-data"
    volume_root="/data"
    service="caddy"
  fi

  local snapshot_path="$volume_root"
  [ -n "$rel_path" ] && snapshot_path="${volume_root%/}/${rel_path#/}"

  local staging_dir
  staging_dir="${BACKUP_STAGING_DIR%/}/${target}-restore"
  rm -rf -- "$staging_dir"
  mkdir -p "$staging_dir" && chmod 700 "$staging_dir"
  cleanup_on_exit "$staging_dir"

  log "restoring snapshot '${snapshot}' (tag=${tag}) path '${snapshot_path}' into staging"
  restic_run -v "${staging_dir}:/restore-target" -- \
    restore "$snapshot" --tag "$tag" --host contractor-platform \
    --target /restore-target --include "$snapshot_path"

  local restored_root="${staging_dir}${volume_root}"
  [ -d "$restored_root" ] || die "nothing restored under ${volume_root} — check the snapshot/path"

  log "validating restored content (non-empty files)"
  local empty_count
  empty_count="$(find "$restored_root" -type f -empty | wc -l | tr -d ' ')"
  if [ "$empty_count" -gt 0 ]; then
    log "WARNING: ${empty_count} restored file(s) are zero-byte — inspect before trusting this restore:"
    find "$restored_root" -type f -empty >&2
  fi
  log "restored file listing:"
  find "$restored_root" -type f >&2

  if [ "$confirmed" -ne 1 ]; then
    log "DRY RUN — content staged at ${restored_root}, real volume untouched. Re-run with --confirm to copy it in."
    exit 2
  fi

  local cid
  cid="$(compose_container_id "$service")"

  log "copying validated content into the real ${target} volume"
  if [ "$target" = "backend-storage" ]; then
    docker run --rm \
      --volumes-from "$cid" \
      -v "${restored_root}:/restore-src:ro" \
      alpine:3.20 \
      sh -c 'cp -a /restore-src/. "$1"/ && chown -R "$2":"$3" "$1"' \
      _ "$volume_root" "${BACKUP_BACKEND_UID:-1000}" "${BACKUP_BACKEND_GID:-1000}"
  else
    docker run --rm \
      --volumes-from "$cid" \
      -v "${restored_root}:/restore-src:ro" \
      alpine:3.20 \
      sh -c 'cp -a /restore-src/. "$1"/' \
      _ "$volume_root"
  fi

  log "restore complete — ${service} continued running throughout (files were copied in-place)"
}

main "$@"
