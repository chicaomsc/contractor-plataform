import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./errors";
import { getPublicSite } from "./public-site";

const originalEnv = process.env;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  process.env.NEXT_PUBLIC_COMPANY_SLUG = "tenant-slug";
});

afterEach(() => {
  process.env = { ...originalEnv };
  vi.restoreAllMocks();
});

describe("public site API", () => {
  it("requests the site endpoint by slug", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      Response.json({
        slug: "tenant-slug",
        name: "Tenant",
        tradeName: null,
        publicPhone: null,
        whatsapp: null,
        website: null,
        location: null,
        branding: null,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getPublicSite("tenant-slug")).resolves.toMatchObject({
      slug: "tenant-slug",
    });
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("/public/sites/tenant-slug", "http://api.test"),
      expect.objectContaining({
        headers: expect.objectContaining({ Accept: "application/json" }),
      }),
    );
  });

  it("normalizes API failures into ApiError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ detail: "Not found" }), {
          status: 404,
          headers: { "content-type": "application/json" },
        }),
      ),
    );

    await expect(getPublicSite("missing")).rejects.toBeInstanceOf(ApiError);
  });

  it("rejects invalid public site responses", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json({ name: "Tenant" })),
    );

    await expect(getPublicSite("tenant-slug")).rejects.toMatchObject({
      code: "invalid-response",
    });
  });
});
