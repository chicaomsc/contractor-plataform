import { afterEach, describe, expect, it } from "vitest";
import { buildTenantLandingUrl } from "./tenant-landing-url";

const originalEnv = process.env;

afterEach(() => {
  process.env = { ...originalEnv };
});

describe("buildTenantLandingUrl", () => {
  it("builds localhost subdomain URLs with a port", () => {
    expect(
      buildTenantLandingUrl("teste", {
        siteUrl: "http://localhost:3001",
        platformBaseDomain: "localhost",
      }),
    ).toBe("http://teste.localhost:3001/");
  });

  it("builds localhost subdomain URLs without a port", () => {
    expect(
      buildTenantLandingUrl("teste", {
        siteUrl: "http://localhost",
        platformBaseDomain: "localhost",
      }),
    ).toBe("http://teste.localhost/");
  });

  it("builds production HTTPS URLs with the configured platform base domain", () => {
    expect(
      buildTenantLandingUrl("teste", {
        siteUrl: "https://app.example.com",
        platformBaseDomain: "example.com",
      }),
    ).toBe("https://teste.example.com/");
  });

  it("builds production HTTP URLs when the platform origin is HTTP", () => {
    expect(
      buildTenantLandingUrl("teste", {
        siteUrl: "http://app.example.com",
        platformBaseDomain: "example.com",
      }),
    ).toBe("http://teste.example.com/");
  });

  it("returns different URLs for different slugs", () => {
    const options = {
      siteUrl: "http://localhost:3001",
      platformBaseDomain: "localhost",
    };

    expect(buildTenantLandingUrl("tenant-a", options)).not.toBe(
      buildTenantLandingUrl("tenant-b", options),
    );
  });

  it("keeps the current origin when the platform base domain is not configured", () => {
    expect(
      buildTenantLandingUrl("teste", {
        siteUrl: "http://localhost:3001",
        platformBaseDomain: "",
      }),
    ).toBe("http://localhost:3001/");
  });

  it("does not depend on a build-time company slug", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    process.env.NEXT_PUBLIC_SITE_URL = "http://localhost:3001";
    process.env.NEXT_PUBLIC_PLATFORM_BASE_DOMAIN = "localhost";
    process.env["NEXT_PUBLIC_" + "COMPANY_" + "SLUG"] = "empresa-errada";

    expect(buildTenantLandingUrl("empresa-certa")).toBe(
      "http://empresa-certa.localhost:3001/",
    );
  });
});
