import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { adminApiRequestBlob } from "./admin-http-client";
import { ApiError } from "./errors";

const originalEnv = process.env;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  process.env.NEXT_PUBLIC_COMPANY_SLUG = "empresa-teste";
});

afterEach(() => {
  process.env = { ...originalEnv };
  vi.restoreAllMocks();
});

function pdfResponse(headers: Record<string, string>) {
  return new Response(new Blob(["%PDF-1.4"], { type: "application/pdf" }), {
    status: 200,
    headers,
  });
}

describe("adminApiRequestBlob", () => {
  it("returns the blob and the filename from a UTF-8 Content-Disposition", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        pdfResponse({
          "Content-Type": "application/pdf",
          "Content-Disposition": "attachment; filename*=UTF-8''or%C3%A7amento-ORC-2026-0001.pdf",
        }),
      ),
    );

    const result = await adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "tok" });

    expect(result.filename).toBe("orçamento-ORC-2026-0001.pdf");
    expect(result.blob.type).toBe("application/pdf");
  });

  it("falls back to the plain filename parameter when there is no UTF-8 variant", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        pdfResponse({
          "Content-Type": "application/pdf",
          "Content-Disposition": 'attachment; filename="orcamento-ORC-2026-0001.pdf"',
        }),
      ),
    );

    const result = await adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "tok" });

    expect(result.filename).toBe("orcamento-ORC-2026-0001.pdf");
  });

  it("returns a null filename when Content-Disposition is absent", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(pdfResponse({ "Content-Type": "application/pdf" })));

    const result = await adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "tok" });

    expect(result.filename).toBeNull();
  });

  it("sends the Authorization header with the access token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(pdfResponse({ "Content-Type": "application/pdf" }));
    vi.stubGlobal("fetch", fetchMock);

    await adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "my-token" });

    const [, requestInit] = fetchMock.mock.calls[0] as [URL, RequestInit];
    const headers = requestInit.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer my-token");
  });

  it("throws an ApiError with the response status on failure (e.g. cross-tenant 404)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ title: "Not found" }), {
          status: 404,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "tok" })).rejects.toBeInstanceOf(
      ApiError,
    );
  });
});
