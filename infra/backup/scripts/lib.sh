#!/usr/bin/env bash
# Contractor Platform — shared helpers for infra/backup/scripts/*.sh.
#
# Not one of the five scripts named in docs/design/DT-011A.5-backup-restore.md
# §25 — added during implementation because backup-postgres.sh, backup-files.sh,
# backup-all.sh, restore-postgres.sh and restore-files.sh all need the exact same
# logging, env-loading and Restic-invocation logic; duplicating it five times
# would itself violate the DT's own "evite scripts gigantes" instruction. Sourced
# by every other script in this directory, never executed directly.
#
# Every function here is deliberately silent about secrets: it never echoes
# POSTGRES_PASSWORD, RESTIC_PASSWORD (file contents) or Storage Box credentials.

set -Eeuo pipefail

# ── Logging ──────────────────────────────────────────────────────────────────

log() {
  printf '%s [%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${SCRIPT_NAME:-backup}" "$*" >&2
}

die() {
  log "ERROR: $*"
  exit 1
}

# ── Precondition helpers ─────────────────────────────────────────────────────

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "required command not found on PATH: $1"
}

require_var() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    die "required variable not set: ${name} (see infra/backup/env/backup.env.example)"
  fi
}

require_file() {
  local path="$1" label="$2"
  [ -f "$path" ] || die "${label} not found: ${path}"
}

# ── Config loading ───────────────────────────────────────────────────────────
# BACKUP_ENV_FILE lets a caller (systemd, a test harness) point at a different
# file; the default resolves infra/backup/env/backup.env relative to this
# script's own directory, so it works the same whether invoked by a human, a
# systemd unit, or this sprint's local validation, without relying on the
# invoker to have exported anything first.

load_backup_env() {
  local lib_dir env_file env_dir
  lib_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  env_file="${BACKUP_ENV_FILE:-${lib_dir}/../env/backup.env}"

  require_file "$env_file" "backup env file"
  env_dir="$(cd "$(dirname "$env_file")" && pwd)"

  # backup.env is plain KEY=VALUE (+ '#' comments/blank lines), the same shape
  # already used by infra/env/production.env(.example) — safe to `source`
  # directly. `set -a` exports every assignment so subsequent docker/docker
  # compose invocations see them without repeating each name.
  set -a
  # shellcheck source=/dev/null
  source "$env_file"
  set +a

  # BACKUP_COMPOSE_FILE/BACKUP_PRODUCTION_ENV are documented as relative to
  # backup.env's OWN directory (infra/backup/env/), not to whatever directory
  # the script happened to be invoked from — resolve that here once, so every
  # script (and a human running one from any CWD) sees a consistent absolute
  # path instead of a relative one that silently breaks outside
  # infra/backup/scripts/.
  if [ -n "${BACKUP_COMPOSE_FILE:-}" ] && [ "${BACKUP_COMPOSE_FILE#/}" = "$BACKUP_COMPOSE_FILE" ]; then
    BACKUP_COMPOSE_FILE="$(cd "$env_dir" && cd "$(dirname "$BACKUP_COMPOSE_FILE")" && pwd)/$(basename "$BACKUP_COMPOSE_FILE")"
  fi
  if [ -n "${BACKUP_PRODUCTION_ENV:-}" ] && [ "${BACKUP_PRODUCTION_ENV#/}" = "$BACKUP_PRODUCTION_ENV" ]; then
    BACKUP_PRODUCTION_ENV="$(cd "$env_dir" && cd "$(dirname "$BACKUP_PRODUCTION_ENV")" && pwd)/$(basename "$BACKUP_PRODUCTION_ENV")"
  fi
}

# ── Compose service resolution ───────────────────────────────────────────────
# Resolves a running container by Compose service name, using the same compose
# file/env the deployment itself uses — never guesses a
# Compose-project-prefixed container/volume name.

compose_container_id() {
  local service="$1" cid
  cid="$(docker compose -f "$BACKUP_COMPOSE_FILE" --env-file "$BACKUP_PRODUCTION_ENV" ps -q "$service")"
  [ -n "$cid" ] || die "service '${service}' has no running container (docker compose ps -q returned nothing)"
  echo "$cid"
}

# ── Restic invocation ────────────────────────────────────────────────────────
# Single place that assembles `docker run` for the pinned Restic image — every
# script calls restic_run instead of building its own docker/restic command
# line, so the repository/password/cache/SFTP wiring only exists once.
#
# Usage: restic_run [extra docker-run args...] -- <restic subcommand + args...>
# Example: restic_run -v "$dir:/src:ro" -- backup /src --tag postgres

restic_run() {
  local -a docker_args=(run --rm -i)
  local -a restic_args=()
  local seen_separator=0

  for arg in "$@"; do
    if [ "$seen_separator" -eq 0 ] && [ "$arg" = "--" ]; then
      seen_separator=1
      continue
    fi
    if [ "$seen_separator" -eq 0 ]; then
      docker_args+=("$arg")
    else
      restic_args+=("$arg")
    fi
  done
  [ "$seen_separator" -eq 1 ] || die "restic_run: missing '--' separator between docker args and restic args"

  require_var RESTIC_REPOSITORY
  require_var RESTIC_PASSWORD_FILE
  require_var RESTIC_IMAGE
  require_var RESTIC_CACHE_DIR
  require_file "$RESTIC_PASSWORD_FILE" "RESTIC_PASSWORD_FILE"

  mkdir -p "$RESTIC_CACHE_DIR"

  docker_args+=(
    -e "RESTIC_REPOSITORY=${RESTIC_REPOSITORY}"
    -e "RESTIC_PASSWORD_FILE=/run/secrets/restic-password"
    -v "${RESTIC_PASSWORD_FILE}:/run/secrets/restic-password:ro"
    -v "${RESTIC_CACHE_DIR}:/root/.cache/restic"
  )

  # A local/path repository (no "scheme:" prefix — sftp:, s3:, b2:, etc. are all
  # network backends needing no mount) only exists on the HOST filesystem; the
  # Restic container needs it bind-mounted at the identical path, or
  # RESTIC_REPOSITORY (as seen from inside the container) points nowhere. This
  # is exactly the local-repository setup used to validate this sprint without
  # a real Storage Box — see infra/backup/README.md "Setup".
  case "$RESTIC_REPOSITORY" in
    *:*) : ;; # scheme-prefixed (sftp:, s3:, rest:, etc.) — network backend, no mount needed
    /*) docker_args+=(-v "${RESTIC_REPOSITORY}:${RESTIC_REPOSITORY}") ;;
    *) die "RESTIC_REPOSITORY must be an absolute path or a scheme-prefixed URL (got: ${RESTIC_REPOSITORY})" ;;
  esac

  # SFTP identity (Hetzner Storage Box, Sprint 11B — unset/untested for the
  # local-repository validation done in this sprint). RESTIC_SFTP_COMMAND, when
  # set, fully replaces Restic's default sftp invocation (needed to point at a
  # specific private key) — see infra/backup/README.md "Hetzner Storage Box".
  if [ -n "${RESTIC_SFTP_KEY_FILE:-}" ]; then
    docker_args+=(-v "${RESTIC_SFTP_KEY_FILE}:/root/.ssh/backup_key:ro")
  fi
  if [ -n "${RESTIC_SFTP_COMMAND:-}" ]; then
    restic_args=(-o "sftp.command=${RESTIC_SFTP_COMMAND}" "${restic_args[@]}")
  fi

  docker_args+=("$RESTIC_IMAGE")

  log "restic ${restic_args[*]}"
  docker "${docker_args[@]}" "${restic_args[@]}"
}

# ── Cleanup registry ─────────────────────────────────────────────────────────
# A single `trap ... EXIT` for the whole process, accumulating every path/
# volume registered by cleanup_on_exit/cleanup_volume_on_exit — calling `trap`
# a second time REPLACES the previous handler rather than adding to it, so
# every script that needs more than one thing cleaned up (e.g.
# backup-postgres.sh: a staging directory AND a transfer volume) must go
# through this registry, never call `trap ... EXIT` directly itself. Fires on
# success, failure, or signal — never leaves a Postgres dump, staged restore
# content, or a transfer volume behind (DT-011A.5 §23).

_CLEANUP_PATHS=()
_CLEANUP_VOLUMES=()

_run_registered_cleanup() {
  local p v
  for p in "${_CLEANUP_PATHS[@]:-}"; do
    [ -n "$p" ] && rm -rf -- "$p"
  done
  for v in "${_CLEANUP_VOLUMES[@]:-}"; do
    [ -n "$v" ] && docker volume rm -f "$v" >/dev/null 2>&1
  done
  return 0
}
trap _run_registered_cleanup EXIT

cleanup_on_exit() {
  _CLEANUP_PATHS+=("$1")
}

cleanup_volume_on_exit() {
  _CLEANUP_VOLUMES+=("$1")
}

# ── Staging → Restic handoff ─────────────────────────────────────────────────
# Backs up the full contents of a host staging directory, tagged postgres.
#
# Copies staging_dir into a short-lived Docker named volume first, instead of
# bind-mounting staging_dir straight into the Restic container. This isn't
# just host-path hygiene: on Docker Desktop for Mac, `restic backup` reading a
# host bind-mounted source file reliably fails with "input/output error" (a
# virtiofs/Restic incompatibility, reproduced in isolation while validating
# this sprint — a plain `cp`/`cat` on the identical bind mount reads it fine;
# only Restic's own read of a BIND-MOUNTED SOURCE fails). Restic reading from
# a named volume, or reading/writing the repository itself via a host bind
# mount, both work without issue — this function sidesteps the one affected
# code path with primitives already proven to work, on every Docker host.
send_staging_to_restic() {
  local staging_dir="$1" xfer_volume
  xfer_volume="contractor-platform-backup-xfer-$$"

  docker volume create "$xfer_volume" >/dev/null
  cleanup_volume_on_exit "$xfer_volume"

  docker run --rm -v "${staging_dir}:/src:ro" -v "${xfer_volume}:/dst" alpine:3.20 \
    sh -c 'cp -a /src/. /dst/'

  log "sending staged content to Restic (tag=postgres)"
  restic_run -v "${xfer_volume}:/staging:ro" -- \
    backup /staging \
    --tag postgres --tag "type=postgres" --tag "db=${POSTGRES_DB:-}" \
    --host contractor-platform
}
