import type { NextConfig } from "next";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const apiUrl = new URL(apiBaseUrl);
const connectSrc = apiUrl.origin;

// Sprint 11A.3 made NEXT_PUBLIC_API_BASE_URL the frontend's own origin and put
// `/api/*` prefix-stripping (frontend/src/lib/api/api-path.ts, withApiPrefix) on
// the assumption that Caddy always sits in front to strip it before forwarding to
// the backend. Caddy also forwards `/uploads/*` to the backend without stripping
// the prefix. Local `next dev` and the Playwright E2E harness don't run Caddy, so
// these rewrites are the local substitute, active only when API_PROXY_TARGET is set.
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
    return [
      { source: "/api/:path*", destination: `${apiProxyTarget}/:path*` },
      {
        source: "/uploads/:path*",
        destination: `${apiProxyTarget}/uploads/:path*`,
      },
    ];
  },
  // SEC-STORAGE-01 (DT-011B.4/§13 HARD-04): a wildcard hostname here turns Next's
  // built-in /_next/image endpoint into an open server-side proxy — any visitor could
  // pass an arbitrary internal URL and have this server fetch it. Every image the app
  // ever renders is one of our own /uploads/** assets (see resolveAdminAssetUrl/
  // resolvePublicAssetUrl), served from this exact same API origin in every
  // environment — so that's the only origin/path allowed, never a wildcard.
  images: {
    remotePatterns: [
      {
        protocol: apiUrl.protocol === "https:" ? "https" : "http",
        hostname: apiUrl.hostname,
        port: apiUrl.port,
        pathname: "/uploads/**",
      },
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
        // HSTS is deliberately NOT set here: it would apply even to plain-HTTP local
        // dev (there's no real TLS termination yet — Sprint 11C). Add it once
        // CADDY_HOST is a real domain, not before (Sprint 11B.6D item 10).
      },
      {
        // Authenticated pages (dashboard, platform admin) must never be cached by the
        // browser/a shared proxy — same intent as the Cache-Control Spring Security
        // already sends by default on every backend API response (Sprint 11B.6D
        // item 10). /admin/login is included too (harmless — a login page doesn't
        // benefit from caching either, and scoping this more finely isn't worth the
        // extra surface for a route pattern that has no cacheable content anyway).
        source: "/(dashboard|admin)/:path*",
        headers: [{ key: "Cache-Control", value: "no-store" }],
      },
    ];
  },
};

export default nextConfig;
