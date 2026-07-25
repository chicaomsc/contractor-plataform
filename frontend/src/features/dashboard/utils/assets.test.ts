import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { resolveAdminAssetUrl } from "./assets";

const originalEnv = process.env;

beforeEach(() => {
  process.env = { ...originalEnv };
});

afterEach(() => {
  process.env = originalEnv;
});

describe("resolveAdminAssetUrl", () => {
  it("resolves a relative /uploads path against NEXT_PUBLIC_API_BASE_URL without an /api prefix", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";

    // /uploads/** is routed directly to the backend by Caddy (no prefix stripped,
    // no /api namespace involved) — see docs/design/DT-011A.3-caddy-reverse-proxy.md
    // §7. This must never become /api/uploads/....
    expect(resolveAdminAssetUrl("/uploads/company/x/logo/y.png")).toBe(
      "http://localhost:8080/uploads/company/x/logo/y.png",
    );
  });

  it("leaves an already-absolute URL untouched", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:80";

    expect(resolveAdminAssetUrl("https://cdn.example.test/logo.png")).toBe(
      "https://cdn.example.test/logo.png",
    );
  });

  it("returns null for a null input", () => {
    expect(resolveAdminAssetUrl(null)).toBeNull();
  });

  it("falls back to the raw path when NEXT_PUBLIC_API_BASE_URL is unset", () => {
    delete process.env.NEXT_PUBLIC_API_BASE_URL;

    expect(resolveAdminAssetUrl("/uploads/company/x/logo/y.png")).toBe(
      "/uploads/company/x/logo/y.png",
    );
  });
});
