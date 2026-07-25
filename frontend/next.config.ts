import type { NextConfig } from "next";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const connectSrc = new URL(apiBaseUrl).origin;

// Sprint 11A.3 made NEXT_PUBLIC_API_BASE_URL the frontend's own origin and put
// `/api/*` prefix-stripping (frontend/src/lib/api/api-path.ts, withApiPrefix) on
// the assumption that Caddy always sits in front to strip it before forwarding to
// the backend (docs/design/DT-011A.3-caddy-reverse-proxy.md). Local `next dev` and
// the Playwright E2E harness (playwright.config.ts) don't run Caddy, so nothing
// else strips that prefix — this rewrite is the substitute, active only when
// API_PROXY_TARGET is set (E2E/local dev). It's never set in the production Docker
// build/compose, so this is a no-op there — and even if it were set, Caddy already
// intercepts every `/api/*` request before it reaches the frontend at all in
// production, so this rewrite would never actually be evaluated.
const apiProxyTarget = process.env.API_PROXY_TARGET;

const nextConfig: NextConfig = {
  // Sprint 11A.1: standalone output for the production Docker image — bundles a
  // minimal server.js with only the traced dependencies, instead of requiring
  // node_modules + npm start in the runtime image.
  output: "standalone",
  poweredByHeader: false,
  reactStrictMode: true,
  async rewrites() {
    if (!apiProxyTarget) {
      return [];
    }
    return [{ source: "/api/:path*", destination: `${apiProxyTarget}/:path*` }];
  },
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "**" },
      { protocol: "http", hostname: "**" },
    ],
  },
  async headers() {
    const contentSecurityPolicy = [
      "default-src 'self'",
      `connect-src 'self' ${connectSrc}`,
      "img-src 'self' data: blob: http: https:",
      "style-src 'self' 'unsafe-inline'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      "font-src 'self' data:",
      "frame-ancestors 'none'",
      "base-uri 'self'",
      "form-action 'self'",
    ].join("; ");

    return [
      {
        source: "/:path*",
        headers: [
          { key: "Content-Security-Policy", value: contentSecurityPolicy },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
};

export default nextConfig;
