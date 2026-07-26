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
