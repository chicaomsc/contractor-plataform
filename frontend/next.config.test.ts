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
