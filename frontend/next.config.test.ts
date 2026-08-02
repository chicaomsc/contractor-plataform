import { afterEach, describe, expect, it, vi } from "vitest";

const originalEnv = process.env;

afterEach(() => {
  process.env = { ...originalEnv };
  vi.resetModules();
});

describe("next.config rewrites", () => {
  it("proxies API and uploads paths to the backend during local development", async () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
    process.env.API_PROXY_TARGET = "http://localhost:8080";
    vi.resetModules();

    const { default: nextConfig } = await import("./next.config");
    const rewrites =
      typeof nextConfig.rewrites === "function"
        ? await nextConfig.rewrites()
        : [];

    expect(rewrites).toEqual([
      { source: "/api/:path*", destination: "http://localhost:8080/:path*" },
      {
        source: "/uploads/:path*",
        destination: "http://localhost:8080/uploads/:path*",
      },
    ]);
  });
});

describe("next.config images.remotePatterns (SEC-STORAGE-01)", () => {
  it("never allows a wildcard hostname, for either protocol", async () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    vi.resetModules();

    const { default: nextConfig } = await import("./next.config");
    const patterns = nextConfig.images?.remotePatterns ?? [];

    expect(patterns.length).toBeGreaterThan(0);
    for (const pattern of patterns) {
      expect("hostname" in pattern ? pattern.hostname : undefined).not.toBe("**");
    }
  });

  it("scopes the remote pattern to the configured API origin and /uploads/** only", async () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.example.com";
    vi.resetModules();

    const { default: nextConfig } = await import("./next.config");
    const patterns = nextConfig.images?.remotePatterns ?? [];

    expect(patterns).toEqual([
      {
        protocol: "https",
        hostname: "api.example.com",
        port: "",
        pathname: "/uploads/**",
      },
    ]);
  });
});

describe("next.config headers (Sprint 11B.6D item 10)", () => {
  it("sends the baseline security headers on every path, with no HSTS", async () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    vi.resetModules();

    const { default: nextConfig } = await import("./next.config");
    const entries =
      typeof nextConfig.headers === "function" ? await nextConfig.headers() : [];
    const baseline = entries.find((entry) => entry.source === "/:path*");

    expect(baseline).toBeDefined();
    const keys = baseline!.headers.map((h) => h.key);
    expect(keys).toEqual(
      expect.arrayContaining([
        "Content-Security-Policy",
        "X-Content-Type-Options",
        "Referrer-Policy",
        "X-Frame-Options",
        "Permissions-Policy",
      ]),
    );
    expect(keys).not.toContain("Strict-Transport-Security");
  });

  it("marks dashboard and admin routes as never cached", async () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    vi.resetModules();

    const { default: nextConfig } = await import("./next.config");
    const entries =
      typeof nextConfig.headers === "function" ? await nextConfig.headers() : [];
    const authenticated = entries.find(
      (entry) => entry.source === "/(dashboard|admin)/:path*",
    );

    expect(authenticated).toBeDefined();
    expect(authenticated!.headers).toEqual(
      expect.arrayContaining([{ key: "Cache-Control", value: "no-store" }]),
    );
  });
});
