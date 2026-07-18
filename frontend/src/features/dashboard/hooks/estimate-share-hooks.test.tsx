import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useCreateEstimateShare, useEstimateShare, useRevokeEstimateShare } from "./estimate-share-hooks";

const originalEnv = process.env;

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    accessToken: "test-access-token",
    session: null,
    isAuthenticated: true,
    isCheckingSession: false,
    login: vi.fn(),
    logout: vi.fn(),
    refetchSession: vi.fn(),
  }),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

function shareResponseFixture(overrides: Record<string, unknown> = {}) {
  return {
    id: "share1",
    status: "ACTIVE",
    token: "raw-token-value",
    createdAt: "2026-07-16T10:00:00Z",
    expiresAt: "2026-08-15T10:00:00Z",
    revokedAt: null,
    lastAccessAt: null,
    accessCount: 0,
    ...overrides,
  };
}

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  process.env.NEXT_PUBLIC_COMPANY_SLUG = "empresa-teste";
});

afterEach(() => {
  process.env = { ...originalEnv };
  vi.restoreAllMocks();
});

describe("useCreateEstimateShare", () => {
  it("POSTs to /estimates/{id}/share and returns the one-time raw token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json(shareResponseFixture(), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => useCreateEstimateShare("e1"), { wrapper: createWrapper() });
    result.current.mutate({});

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const [url, requestInit] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(String(url)).toContain("/estimates/e1/share");
    expect(requestInit.method).toBe("POST");
    expect(result.current.data?.token).toBe("raw-token-value");
  });
});

describe("useEstimateShare", () => {
  it("GETs the current share status without a raw token", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json(shareResponseFixture({ token: null }))),
    );

    const { result } = renderHook(() => useEstimateShare("e1"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.status).toBe("ACTIVE");
    expect(result.current.data?.token).toBeNull();
  });

  it("surfaces a 404 (no share created yet) without retrying", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ title: "Not found" }), {
          status: 404,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    const { result } = renderHook(() => useEstimateShare("e1"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

describe("useRevokeEstimateShare", () => {
  it("DELETEs the share", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => useRevokeEstimateShare("e1"), { wrapper: createWrapper() });
    result.current.mutate();

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const [url, requestInit] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(String(url)).toContain("/estimates/e1/share");
    expect(requestInit.method).toBe("DELETE");
  });
});
