import { describe, expect, it } from "vitest";
import { withApiPrefix } from "./api-path";

describe("withApiPrefix", () => {
  it("prefixes a plain path with /api", () => {
    expect(withApiPrefix("/estimates")).toBe("/api/estimates");
    expect(withApiPrefix("/auth/login")).toBe("/api/auth/login");
    expect(withApiPrefix("/public/sites/jr-pinturas")).toBe("/api/public/sites/jr-pinturas");
  });

  it("is idempotent — never doubles an already-prefixed path", () => {
    expect(withApiPrefix("/api/estimates")).toBe("/api/estimates");
    expect(withApiPrefix("/api")).toBe("/api");
  });

  it("does not touch /uploads paths passed through unrelated code paths", () => {
    // withApiPrefix is only ever applied to JSON API calls, never to /uploads/**
    // asset URLs (those are resolved by resolveAdminAssetUrl/resolvePublicAssetUrl,
    // a separate code path) — this test documents that this function itself has no
    // special-casing for /uploads, since it is simply never called with one.
    expect(withApiPrefix("/uploads/company/x/logo/y.png")).toBe(
      "/api/uploads/company/x/logo/y.png",
    );
  });
});
