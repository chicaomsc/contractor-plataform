# syntax=docker/dockerfile:1

# Contractor Platform — backend production image.
#
# Java 25 (pom.xml <java.version>) via Eclipse Temurin, matching the CI toolchain
# (.github/workflows/backend-ci.yml: actions/setup-java@v4, distribution "temurin").
# Availability of eclipse-temurin:25-jdk-alpine / 25-jre-alpine for linux/amd64 was
# verified against the registry before choosing these tags.

# ── Stage 1: dependency cache ───────────────────────────────────────────────
# Only pom.xml + the Maven Wrapper are copied here, so this layer (and its downloaded
# dependency cache) only invalidates when dependencies actually change, not on every
# source edit.
FROM eclipse-temurin:25-jdk-alpine AS deps
WORKDIR /build

COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# ── Stage 2: build ───────────────────────────────────────────────────────────
FROM deps AS build
WORKDIR /build

COPY src/ src/

# Tests are intentionally skipped here, not silently: the full suite requires
# Testcontainers, which needs access to a Docker daemon. Mounting docker.sock into
# the build container is explicitly out of scope for this image (see infra/README.md
# security notes), and the test suite already runs in CI (backend-ci.yml) before an
# image is ever built from this Dockerfile. `-DskipTests` here only skips re-running
# already-verified tests during packaging, it does not replace CI verification.
RUN ./mvnw -B -ntp -DskipTests package \
    && mv target/platform-backend-*.jar target/app.jar

# ── Stage 3: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

# Fixed UID/GID for portability across hosts/orchestrators (Hetzner today, ECS/Fargate
# later) where a stable, known non-root identity is preferable to a dynamically
# allocated one.
RUN addgroup -g 1000 platform && adduser -u 1000 -G platform -D -h /app platform

WORKDIR /app

COPY --from=build --chown=platform:platform /build/target/app.jar app.jar

# /app/storage is StorageProperties' default base-path (app.storage.base-path,
# overridable via STORAGE_PATH) — the only directory this application writes
# uploaded files to. Created here so it exists (and is writable by the non-root
# user) even before a volume is mounted over it.
RUN mkdir -p /app/storage && chown -R platform:platform /app/storage

USER platform

ENV STORAGE_PATH=/app/storage \
    PORT=8080

EXPOSE 8080

# eclipse-temurin:*-alpine ships BusyBox wget out of the box — no extra package
# installed just for this check. /actuator/health is permitAll in SecurityConfig.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

# Exec form: java runs as PID 1 and receives SIGTERM directly from `docker stop`/
# Compose, so Spring Boot's shutdown hook runs instead of the signal being swallowed
# by an intermediate shell. JAVA_TOOL_OPTIONS (e.g. -XX:MaxRAMPercentage=...) is read
# automatically by the JVM from the environment — no shell interpolation needed here,
# which is what keeps this exec form valid.
ENTRYPOINT ["java", "-jar", "app.jar"]
