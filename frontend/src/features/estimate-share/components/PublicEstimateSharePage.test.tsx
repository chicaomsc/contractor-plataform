import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PublicEstimateSharePage } from "./PublicEstimateSharePage";

const originalEnv = process.env;

let queryState: { isLoading: boolean; isError: boolean; data: unknown };

vi.mock("../hooks/estimate-share-hooks", () => ({
  usePublicEstimateShare: () => queryState,
}));

function shareFixture(overrides: Record<string, unknown> = {}) {
  return {
    seller: {
      displayName: "Acme Reformas",
      logoUrl: "/uploads/company/x/logo.png",
      phone: "912345678",
      email: "hello@acme.example",
      website: "https://acme.example",
      addressLine: "Rua Principal 1, Lisboa",
    },
    estimate: {
      number: "ORC-2026-0001",
      title: "Pintura interior",
      description: "Pintura de 3 divisões",
      status: "Enviado",
      draft: false,
      cancelled: false,
      issueDate: "16/07/2026",
      validUntil: "15/08/2026",
    },
    customer: { name: "Jane Doe" },
    items: [
      { description: "Pintura", quantity: "2", unit: "m²", unitPrice: "50,00 €", total: "100,00 €" },
    ],
    materials: [],
    summary: {
      currency: "EUR",
      laborSubtotal: "100,00 €",
      materialSubtotal: "0,00 €",
      subtotal: "100,00 €",
      vatLabel: "IVA (23%)",
      vatAmount: "23,00 €",
      total: "123,00 €",
      upfrontLabel: "Entrada (50%)",
      upfrontAmount: "61,50 €",
      remaining: "61,50 €",
    },
    notes: "Trazer proteção para o chão.",
    terms: "Pagamento em 2 tranches.",
    ...overrides,
  };
}

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  process.env.NEXT_PUBLIC_COMPANY_SLUG = "empresa-teste";
  queryState = { isLoading: false, isError: false, data: shareFixture() };
});

afterEach(() => {
  process.env = { ...originalEnv };
  vi.restoreAllMocks();
});

describe("PublicEstimateSharePage", () => {
  it("shows a loading state while fetching", () => {
    queryState = { isLoading: true, isError: false, data: undefined };
    render(<PublicEstimateSharePage token="abc123" />);

    expect(screen.getByText(/A carregar orçamento partilhado/)).toBeInTheDocument();
  });

  it("shows an unavailable message on error (unknown/expired/revoked token)", () => {
    queryState = { isLoading: false, isError: true, data: undefined };
    render(<PublicEstimateSharePage token="abc123" />);

    expect(screen.getByText("Link indisponível")).toBeInTheDocument();
  });

  it("renders the seller, customer, items, and financial summary from the response verbatim", () => {
    render(<PublicEstimateSharePage token="abc123" />);

    expect(screen.getAllByText("Acme Reformas").length).toBeGreaterThan(0);
    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    expect(screen.getByText("Pintura")).toBeInTheDocument();
    expect(screen.getByText("123,00 €")).toBeInTheDocument();
    expect(screen.getByText("Trazer proteção para o chão.")).toBeInTheDocument();
    expect(screen.getByText("Pagamento em 2 tranches.")).toBeInTheDocument();
  });

  it("never renders an internal identifier — only the fields the public DTO exposes", () => {
    render(<PublicEstimateSharePage token="abc123" />);
    const body = document.body.textContent ?? "";

    expect(body).not.toMatch(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/);
  });

  it("links the PDF button to the token-scoped public download endpoint", () => {
    render(<PublicEstimateSharePage token="abc123" />);

    expect(screen.getByRole("link", { name: /Baixar PDF/ })).toHaveAttribute(
      "href",
      "http://api.test/public/share/abc123/pdf",
    );
  });

  it("shows a draft notice when the estimate is still a draft", () => {
    queryState = {
      isLoading: false,
      isError: false,
      data: shareFixture({ estimate: { ...shareFixture().estimate, draft: true } }),
    };
    render(<PublicEstimateSharePage token="abc123" />);

    expect(screen.getByText(/ainda está em rascunho/)).toBeInTheDocument();
  });

  it("shows a cancelled notice when the estimate was cancelled", () => {
    queryState = {
      isLoading: false,
      isError: false,
      data: shareFixture({ estimate: { ...shareFixture().estimate, cancelled: true } }),
    };
    render(<PublicEstimateSharePage token="abc123" />);

    expect(screen.getByText(/foi cancelado/)).toBeInTheDocument();
  });
});
