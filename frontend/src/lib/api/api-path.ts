/**
 * Prefixes an API path with `/api` so it routes through Caddy's same-origin
 * `/api/*` reverse-proxy rule to the backend — Caddy strips the `/api` prefix
 * before forwarding, so the Spring controllers themselves are never prefixed
 * (see docs/design/DT-011A.3-caddy-reverse-proxy.md §7). `NEXT_PUBLIC_API_BASE_URL`
 * stays a plain origin (e.g. `http://localhost:80`); this is the only place the
 * `/api` segment is introduced.
 *
 * Idempotent — an already-prefixed path is returned unchanged, so a caller can
 * never end up with a doubled `/api/api/...`.
 */
export function withApiPrefix(path: string): string {
  return path === "/api" || path.startsWith("/api/") ? path : `/api${path}`;
}
