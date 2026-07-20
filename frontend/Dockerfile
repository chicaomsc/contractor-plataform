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
# time — this is a Next.js constraint, not a choice made here (see infra/README.md
# for the full explanation). Given this deployment currently serves a single tenant,
# baking them in at build time (rather than adding a runtime-rewrite entrypoint
# script) is the proportionate choice. Changing any of these three values requires
# rebuilding the image, not just restarting the container.
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_COMPANY_SLUG
ARG NEXT_PUBLIC_SITE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL} \
    NEXT_PUBLIC_COMPANY_SLUG=${NEXT_PUBLIC_COMPANY_SLUG} \
    NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL} \
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

# No dedicated health endpoint exists yet in the app (see infra/README.md — tracked
# as a Sprint 11A.2 follow-up). Checking "/" is a deliberate, documented stand-in:
# it is public (no auth), always resolves through the same Next.js server process,
# and node:22-alpine already ships BusyBox wget, so no extra package is installed.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:3000/ || exit 1

# Exec form + standalone server.js as PID 1: no "npm start" wrapper, no npm install
# at container startup. Node's default SIGTERM handling closes the HTTP server
# directly since there is no intermediate shell process to swallow the signal.
ENTRYPOINT ["node", "server.js"]
