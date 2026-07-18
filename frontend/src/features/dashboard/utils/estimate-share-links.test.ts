import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { buildEstimateShareUrl, buildEstimateShareWhatsAppHref } from "./estimate-share-links";

const originalEnv = process.env;

beforeEach(() => {
  process.env = { ...originalEnv };
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  process.env.NEXT_PUBLIC_COMPANY_SLUG = "empresa-teste";
});

afterEach(() => {
  process.env = originalEnv;
});

describe("buildEstimateShareUrl", () => {
  it("uses NEXT_PUBLIC_SITE_URL when configured", () => {
    process.env.NEXT_PUBLIC_SITE_URL = "https://minhaempresa.example";
    expect(buildEstimateShareUrl("abc123")).toBe("https://minhaempresa.example/share/abc123");
  });

  it("falls back to the current origin when NEXT_PUBLIC_SITE_URL is not set", () => {
    delete process.env.NEXT_PUBLIC_SITE_URL;
    expect(buildEstimateShareUrl("abc123")).toBe(`${window.location.origin}/share/abc123`);
  });

  it("never uses a predictable path like /estimate/{id}", () => {
    const url = buildEstimateShareUrl("xh83JSkLm82A");
    expect(url).toContain("/share/xh83JSkLm82A");
    expect(url).not.toContain("/estimate/");
  });
});

describe("buildEstimateShareWhatsAppHref", () => {
  it("builds a wa.me link with the share URL embedded, with no phone number", () => {
    const href = buildEstimateShareWhatsAppHref("https://site.example/share/abc123");

    expect(href.startsWith("https://wa.me/?text=")).toBe(true);
    expect(decodeURIComponent(href.split("?text=")[1])).toContain(
      "https://site.example/share/abc123",
    );
  });
});
