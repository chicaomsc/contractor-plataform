import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { CreateEstimateInput, UpdateEstimateInput } from "../types/estimates";
import { useCreateEstimate, useEstimate, useEstimates, useUpdateEstimate } from "./estimate-hooks";

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

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  process.env.NEXT_PUBLIC_COMPANY_SLUG = "empresa-teste";
});

afterEach(() => {
  process.env = { ...originalEnv };
  vi.restoreAllMocks();
});

function estimateResponseFixture(overrides: Record<string, unknown> = {}) {
  return {
    id: "e1",
    companyId: "c1",
    customerId: "cust1",
    number: "ORC-2026-0001",
    title: "Pintura interior",
    description: null,
    status: "DRAFT",
    issueDate: "2026-07-16",
    validUntil: "2026-08-15",
    expectedStartDate: null,
    estimatedDurationDays: null,
    notes: null,
    terms: null,
    currency: "EUR",
    vatRate: 23,
    laborSubtotal: 100,
    materialSubtotal: 0,
    subtotal: 100,
    vatAmount: 23,
    total: 123,
    upfrontPercentage: 50,
    upfrontAmount: 61.5,
    remainingAmount: 61.5,
    items: [],
    materials: [],
    createdAt: "2026-07-16T10:00:00Z",
    updatedAt: "2026-07-16T10:00:00Z",
    ...overrides,
  };
}

describe("estimate hooks — creation", () => {
  it("useCreateEstimate posts the payload and never sends client-computed totals", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json(estimateResponseFixture()));
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => useCreateEstimate(), { wrapper: createWrapper() });

    const payload: CreateEstimateInput = {
      customerId: "cust1",
      title: "Pintura interior",
      description: null,
      validUntil: null,
      notes: null,
      terms: null,
      items: [
        { serviceId: null, description: "Pintura", quantity: 2, unit: "M2", unitPrice: 50 },
      ],
      materials: [],
    };

    result.current.mutate(payload);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const [, requestInit] = fetchMock.mock.calls[0] as [URL, RequestInit];
    const sentBody = JSON.parse(requestInit.body as string);

    expect(requestInit.method).toBe("POST");
    expect(sentBody).toEqual(payload);
    expect(sentBody).not.toHaveProperty("total");
    expect(sentBody).not.toHaveProperty("number");
    expect(result.current.data?.number).toBe("ORC-2026-0001");
    expect(result.current.data?.total).toBe(123);
  });
});

describe("estimate hooks — edition", () => {
  it("useUpdateEstimate sends a PUT with the full items/materials replacement", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(Response.json(estimateResponseFixture({ title: "Novo título" })));
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => useUpdateEstimate(), { wrapper: createWrapper() });

    const payload: UpdateEstimateInput = {
      customerId: "cust1",
      title: "Novo título",
      description: null,
      validUntil: null,
      notes: null,
      terms: null,
      items: [],
      materials: [],
    };

    result.current.mutate({ estimateId: "e1", payload });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const [url, requestInit] = fetchMock.mock.calls[0] as [URL, RequestInit];

    expect(String(url)).toContain("/estimates/e1");
    expect(requestInit.method).toBe("PUT");
    expect(result.current.data?.title).toBe("Novo título");
  });

  it("useEstimate fetches the full detail including items and materials", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(estimateResponseFixture())));

    const { result } = renderHook(() => useEstimate("e1"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.id).toBe("e1");
  });

  it("useEstimates fetches the lightweight summary list", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json([
          {
            id: "e1",
            companyId: "c1",
            customerId: "cust1",
            number: "ORC-2026-0001",
            title: "Pintura interior",
            status: "DRAFT",
            issueDate: "2026-07-16",
            validUntil: null,
            currency: "EUR",
            total: 123,
            upfrontAmount: 61.5,
            remainingAmount: 61.5,
            createdAt: "2026-07-16T10:00:00Z",
            updatedAt: "2026-07-16T10:00:00Z",
          },
        ]),
      ),
    );

    const { result } = renderHook(() => useEstimates(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].number).toBe("ORC-2026-0001");
  });
});
