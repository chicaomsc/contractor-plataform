# Contractor Platform — custom Caddy image (Sprint 12.3).
# Design: docs/design/DT-012.1-production-architecture.md §9 / ADR-005.
#
# The official `caddy:2-alpine` image (used through Sprint 12.2) cannot do the
# ACME DNS-01 challenge required for the *.{PLATFORM_BASE_DOMAIN} wildcard
# certificate (see infra/caddy/Caddyfile) — that needs the `caddy-dns/cloudflare`
# module, which isn't part of any official Caddy build. This Dockerfile builds a
# Caddy binary that includes it, via `xcaddy`, with explicit pinned versions for
# both Caddy itself and the plugin (reproducible builds, easy to audit — no
# third-party pre-built Caddy image is used).
#
# No secret is ever part of this image. CLOUDFLARE_API_TOKEN is supplied to the
# *running* container only (docker-compose.prod.yml env_file) and is read by
# Caddy at runtime via the `{env.CLOUDFLARE_API_TOKEN}` placeholder in the
# Caddyfile — never as a build ARG, never baked into a layer.

ARG CADDY_VERSION=2.11.4

FROM caddy:${CADDY_VERSION}-builder-alpine AS builder

# Pinned to an explicit tag (not a branch/latest) for reproducibility — bump
# deliberately, not silently, when a Cloudflare module update is needed.
RUN xcaddy build \
    --with github.com/caddy-dns/cloudflare@v0.2.4

FROM caddy:${CADDY_VERSION}-alpine AS runtime

COPY --from=builder /usr/bin/caddy /usr/bin/caddy
