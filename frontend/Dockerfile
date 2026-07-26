# syntax=docker/dockerfile:1

# Contractor Platform — frontend production image.
#
# next.config.ts sets output: "standalone" (added in Sprint 11A.1 specifically for
# this image) — the runtime stage only needs server.js + .next/static + public,
# not the full node_modules tree.
#
# Package manager: npm (package-lock.json is the only lockfile present in the repo).
# Node 22, matching CI (.github/workflows/frontend-ci.yml: actions/setup-node@v4,
# node-version: '22').

# ── Stage 1: dependencies ────────────────────────────────────────────────────
FROM node:22-alpine AS deps
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

# ── Stage 2: build ────────────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

# NEXT_PUBLIC_* variables are inlined into the client JS bundle by Next.js at build
# time. Tenant identity is intentionally not included here: the public landing
# resolves the tenant at runtime via GET /api/public/tenant.
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_SITE_URL
ARG NEXT_PUBLIC_PLATFORM_BASE_DOMAIN
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL} \
    NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL} \
    NEXT_PUBLIC_PLATFORM_BASE_DOMAIN=${NEXT_PUBLIC_PLATFORM_BASE_DOMAIN} \
    NODE_ENV=production

COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ── Stage 3: runtime ──────────────────────────────────────────────────────────
FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0

# node:22-alpine already ships an unprivileged "node" user/group (uid/gid 1000) —
# reused instead of creating a new one.
COPY --from=build --chown=node:node /app/public ./public
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static

USER node

EXPOSE 3000

# Dedicated health route (Sprint 11A.2) — src/app/api/health/route.ts. Trivial by
# design: no backend call, no data fetch, so it never reports unhealthy because of
# something unrelated to the Next.js process itself (e.g. the public landing page's
# own rendering). node:22-alpine already ships BusyBox wget, so no extra package.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:3000/api/health || exit 1

# Exec form + standalone server.js as PID 1: no "npm start" wrapper, no npm install
# at container startup. Node's default SIGTERM handling closes the HTTP server
# directly since there is no intermediate shell process to swallow the signal.
ENTRYPOINT ["node", "server.js"]
