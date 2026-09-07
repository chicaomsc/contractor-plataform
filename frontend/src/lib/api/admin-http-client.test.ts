import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  SESSION_EXPIRED_EVENT,
  markSessionActive,
} from "@/features/auth/api/session-lifecycle";
import { adminApiRequest, adminApiRequestBlob } from "./admin-http-client";
import { ApiError } from "./errors";

const originalEnv = process.env;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  markSessionActive();
  window.localStorage.clear();
});

afterEach(() => {
  process.env = { ...originalEnv };
  vi.restoreAllMocks();
  markSessionActive();
  window.localStorage.clear();
});

function pdfResponse(headers: Record<string, string>) {
  return new Response(new Blob(["%PDF-1.4"], { type: "application/pdf" }), {
    status: 200,
    headers,
  });
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function authResponse(accessToken: string, refreshToken: string) {
  return {
    accessToken,
    refreshToken,
    user: {
      id: "user-1",
      companyId: "company-1",
      email: "owner@example.com",
      name: "Owner",
      role: "OWNER",
      status: "ACTIVE",
    },
    company: {
      id: "company-1",
      name: "Company",
      slug: "company",
      email: null,
      country: null,
      status: "ACTIVE",
    },
  };
}

describe("adminApiRequestBlob", () => {
  it("returns the blob and the filename from a UTF-8 Content-Disposition", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        pdfResponse({
          "Content-Type": "application/pdf",
          "Content-Disposition":
            "attachment; filename*=UTF-8''or%C3%A7amento-ORC-2026-0001.pdf",
        }),
      ),
    );

    const result = await adminApiRequestBlob("/estimates/e1/pdf", {
      accessToken: "tok",
    });

    expect(result.filename).toBe("orçamento-ORC-2026-0001.pdf");
    expect(result.blob.type).toBe("application/pdf");
  });

  it("falls back to the plain filename parameter when there is no UTF-8 variant", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        pdfResponse({
          "Content-Type": "application/pdf",
          "Content-Disposition":
            'attachment; filename="orcamento-ORC-2026-0001.pdf"',
        }),
      ),
    );

    const result = await adminApiRequestBlob("/estimates/e1/pdf", {
      accessToken: "tok",
    });

    expect(result.filename).toBe("orcamento-ORC-2026-0001.pdf");
  });

  it("returns a null filename when Content-Disposition is absent", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(pdfResponse({ "Content-Type": "application/pdf" })),
    );

    const result = await adminApiRequestBlob("/estimates/e1/pdf", {
      accessToken: "tok",
    });

    expect(result.filename).toBeNull();
  });

  it("sends the Authorization header with the access token", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(pdfResponse({ "Content-Type": "application/pdf" }));
    vi.stubGlobal("fetch", fetchMock);

    await adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "my-token" });

    const [, requestInit] = fetchMock.mock.calls[0] as [URL, RequestInit];
    const headers = requestInit.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer my-token");
  });

  it("prefixes the path with /api so Caddy's same-origin rule routes it to the backend", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(pdfResponse({ "Content-Type": "application/pdf" }));
    vi.stubGlobal("fetch", fetchMock);

    await adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "tok" });

    const [url] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(url.toString()).toBe("http://api.test/api/estimates/e1/pdf");
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

    await expect(
      adminApiRequestBlob("/estimates/e1/pdf", { accessToken: "tok" }),
    ).rejects.toBeInstanceOf(ApiError);
  });
});

describe("adminApiRequest silent refresh", () => {
  it("refreshes on 401 and retries the original request once", async () => {
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401))
      .mockResolvedValueOnce(
        jsonResponse(authResponse("access-new", "refresh-new")),
      )
      .mockResolvedValueOnce(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      adminApiRequest<{ ok: boolean }>("/projects", {
        accessToken: "access-old",
      }),
    ).resolves.toEqual({ ok: true });

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect((fetchMock.mock.calls[1][0] as URL).pathname).toBe(
      "/api/auth/refresh",
    );
    expect(
      (fetchMock.mock.calls[2][1].headers as Headers).get("Authorization"),
    ).toBe("Bearer access-new");
  });

  it("single-flights 10 concurrent 401 responses through one refresh", async () => {
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const fetchMock = vi.fn((url: URL, init: RequestInit) => {
      if (url.pathname === "/api/auth/refresh") {
        return Promise.resolve(
          jsonResponse(authResponse("access-new", "refresh-new")),
        );
      }

      const authorization = (init.headers as Headers).get("Authorization");
      return Promise.resolve(
        authorization === "Bearer access-new"
          ? jsonResponse({ ok: true })
          : jsonResponse({ title: "Unauthorized" }, 401),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    const results = await Promise.all(
      Array.from({ length: 10 }, (_, index) =>
        adminApiRequest<{ ok: boolean }>(`/projects/${index}`, {
          accessToken: "access-old",
        }),
      ),
    );

    expect(results).toHaveLength(10);
    expect(
      fetchMock.mock.calls.filter(
        ([url]) => (url as URL).pathname === "/api/auth/refresh",
      ),
    ).toHaveLength(1);
  });

  it("stores rotated access and refresh tokens from refresh success", async () => {
    window.localStorage.setItem("contractor.accessToken", "access-old");
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401))
        .mockResolvedValueOnce(
          jsonResponse(authResponse("access-new", "refresh-new")),
        )
        .mockResolvedValueOnce(jsonResponse({ ok: true })),
    );

    await adminApiRequest("/settings", { accessToken: "access-old" });

    expect(window.localStorage.getItem("contractor.accessToken")).toBe(
      "access-new",
    );
    expect(window.localStorage.getItem("contractor.refreshToken")).toBe(
      "refresh-new",
    );
  });

  it("clears credentials and emits SESSION_EXPIRED when refresh returns 422", async () => {
    window.localStorage.setItem("contractor.accessToken", "access-old");
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const expiredListener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401))
        .mockResolvedValueOnce(jsonResponse({ title: "Invalid refresh" }, 422)),
    );

    await expect(
      adminApiRequest("/projects", { accessToken: "access-old" }),
    ).rejects.toBeInstanceOf(ApiError);

    expect(window.localStorage.getItem("contractor.accessToken")).toBeNull();
    expect(window.localStorage.getItem("contractor.refreshToken")).toBeNull();
    expect(expiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });

  it("clears credentials and emits SESSION_EXPIRED when refresh returns 401", async () => {
    window.localStorage.setItem("contractor.accessToken", "access-old");
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const expiredListener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401))
        .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401)),
    );

    await expect(
      adminApiRequest("/projects", { accessToken: "access-old" }),
    ).rejects.toBeInstanceOf(ApiError);

    expect(window.localStorage.getItem("contractor.accessToken")).toBeNull();
    expect(window.localStorage.getItem("contractor.refreshToken")).toBeNull();
    expect(expiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });

  it("does not call refresh endpoint when refresh token is absent", async () => {
    const expiredListener = vi.fn();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401));
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      adminApiRequest("/projects", { accessToken: "access-old" }),
    ).rejects.toBeInstanceOf(ApiError);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(expiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });

  it("does not attempt a second refresh when retry also returns 401", async () => {
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const expiredListener = vi.fn();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401))
      .mockResolvedValueOnce(
        jsonResponse(authResponse("access-new", "refresh-new")),
      )
      .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401));
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      adminApiRequest("/projects", { accessToken: "access-old" }),
    ).rejects.toBeInstanceOf(ApiError);

    expect(
      fetchMock.mock.calls.filter(
        ([url]) => (url as URL).pathname === "/api/auth/refresh",
      ),
    ).toHaveLength(1);
    expect(expiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });

  it("does not refresh or emit SESSION_EXPIRED for 403", async () => {
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const expiredListener = vi.fn();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ title: "Forbidden" }, 403));
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      adminApiRequest("/projects", { accessToken: "access-old" }),
    ).rejects.toMatchObject({ status: 403 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(expiredListener).not.toHaveBeenCalled();
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });

  it("does not refresh public auth routes", async () => {
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const expiredListener = vi.fn();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ title: "Unauthorized" }, 401));
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      adminApiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: "a@b.test", password: "secret" }),
      }),
    ).rejects.toMatchObject({ status: 401 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(expiredListener).not.toHaveBeenCalled();
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });

  it("expires concurrent refresh failures consistently with one event", async () => {
    window.localStorage.setItem("contractor.accessToken", "access-old");
    window.localStorage.setItem("contractor.refreshToken", "refresh-old");
    const expiredListener = vi.fn();
    const fetchMock = vi.fn((url: URL) =>
      Promise.resolve(
        url.pathname === "/api/auth/refresh"
          ? jsonResponse({ title: "Invalid refresh" }, 422)
          : jsonResponse({ title: "Unauthorized" }, 401),
      ),
    );
    window.addEventListener(SESSION_EXPIRED_EVENT, expiredListener);
    vi.stubGlobal("fetch", fetchMock);

    const results = await Promise.allSettled(
      Array.from({ length: 10 }, (_, index) =>
        adminApiRequest(`/projects/${index}`, { accessToken: "access-old" }),
      ),
    );

    expect(results.every((result) => result.status === "rejected")).toBe(true);
    expect(
      fetchMock.mock.calls.filter(
        ([url]) => (url as URL).pathname === "/api/auth/refresh",
      ),
    ).toHaveLength(1);
    expect(window.localStorage.getItem("contractor.accessToken")).toBeNull();
    expect(window.localStorage.getItem("contractor.refreshToken")).toBeNull();
    expect(expiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener(SESSION_EXPIRED_EVENT, expiredListener);
  });
});
