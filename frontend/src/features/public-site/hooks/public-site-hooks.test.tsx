import React, { type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/errors";
import {
  usePublicGallery,
  usePublicServices,
  usePublicSite,
} from "./public-site-hooks";

const originalEnv = process.env;

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
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

describe("public site hooks", () => {
  it("usePublicSite loads and maps site data", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          slug: "empresa-teste",
          name: "Empresa Teste Lda",
          tradeName: "Empresa Teste",
          publicPhone: null,
          whatsapp: null,
          website: null,
          location: null,
          branding: null,
        }),
      ),
    );

    const { result } = renderHook(() => usePublicSite("empresa-teste"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.displayName).toBe("Empresa Teste");
  });

  it("usePublicServices loads service view models", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json([
          {
            id: "1",
            name: "Serviço",
            slug: "servico",
            shortDescription: "Resumo",
            description: null,
            icon: null,
            displayOrder: 0,
          },
        ]),
      ),
    );

    const { result } = renderHook(() => usePublicServices("empresa-teste"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.summary).toBe("Resumo");
  });

  it("usePublicGallery preserves 404 as a typed error", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json({ title: "Not found" }, { status: 404 }),
        ),
    );

    const { result } = renderHook(() => usePublicGallery("missing"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeInstanceOf(ApiError);
    expect((result.current.error as ApiError).status).toBe(404);
  });
});
