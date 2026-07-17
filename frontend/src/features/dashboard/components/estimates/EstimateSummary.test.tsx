import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { EstimateDto } from "../../types/estimates";
import { EstimateSummary } from "./EstimateSummary";

function estimateFixture(overrides: Partial<EstimateDto> = {}): EstimateDto {
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
    materialSubtotal: 50,
    subtotal: 150,
    vatAmount: 34.5,
    total: 184.5,
    upfrontPercentage: 50,
    upfrontAmount: 92.25,
    remainingAmount: 92.25,
    items: [],
    materials: [],
    createdAt: "2026-07-16T10:00:00Z",
    updatedAt: "2026-07-16T10:00:00Z",
    ...overrides,
  };
}

describe("EstimateSummary", () => {
  it("renders every financial field exactly as returned by the backend — no recomputation", () => {
    render(<EstimateSummary estimate={estimateFixture()} />);

    // formatMoney(100, "EUR") -> "100,00 €" in pt-PT locale
    expect(screen.getByText(/100,00/)).toBeInTheDocument(); // labor subtotal
    expect(screen.getByText(/^50,00/)).toBeInTheDocument(); // material subtotal
    expect(screen.getByText(/150,00/)).toBeInTheDocument(); // subtotal
    expect(screen.getByText(/34,50/)).toBeInTheDocument(); // vat amount
    expect(screen.getByText(/184,50/)).toBeInTheDocument(); // total
    expect(screen.getAllByText(/92,25/)).toHaveLength(2); // upfront amount + remaining amount
  });

  it("labels VAT and upfront cards with the estimate's own snapshot percentages", () => {
    render(<EstimateSummary estimate={estimateFixture({ vatRate: 6, upfrontPercentage: 30 })} />);

    expect(screen.getByText("IVA (6%)")).toBeInTheDocument();
    expect(screen.getByText("Entrada (30%)")).toBeInTheDocument();
  });

  it("reflects a total of zero for an estimate with no items or materials, without erroring", () => {
    render(
      <EstimateSummary
        estimate={estimateFixture({
          laborSubtotal: 0,
          materialSubtotal: 0,
          subtotal: 0,
          vatAmount: 0,
          total: 0,
          upfrontAmount: 0,
          remainingAmount: 0,
        })}
      />,
    );

    expect(screen.getAllByText(/0,00/).length).toBeGreaterThan(0);
  });
});
