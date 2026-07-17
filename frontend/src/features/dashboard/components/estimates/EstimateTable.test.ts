import { describe, expect, it } from "vitest";
import type { EstimateSummaryDto } from "../../types/estimates";
import { sortEstimates } from "./EstimateTable";

function estimate(overrides: Partial<EstimateSummaryDto> & Pick<EstimateSummaryDto, "id">): EstimateSummaryDto {
  return {
    id: overrides.id,
    companyId: "c1",
    customerId: overrides.customerId ?? "cust1",
    number: overrides.number ?? "ORC-2026-0001",
    title: overrides.title ?? "Orçamento",
    status: overrides.status ?? "DRAFT",
    issueDate: overrides.issueDate ?? "2026-07-16",
    validUntil: overrides.validUntil ?? null,
    currency: "EUR",
    total: overrides.total ?? 100,
    upfrontAmount: overrides.upfrontAmount ?? 50,
    remainingAmount: overrides.remainingAmount ?? 50,
    createdAt: "2026-07-16T10:00:00Z",
    updatedAt: "2026-07-16T10:00:00Z",
  };
}

describe("sortEstimates", () => {
  const customerNameById = new Map([
    ["cust1", "Zeta Obras"],
    ["cust2", "Alfa Obras"],
  ]);

  it("sorts by total ascending and descending", () => {
    const estimates = [
      estimate({ id: "1", total: 300 }),
      estimate({ id: "2", total: 100 }),
      estimate({ id: "3", total: 200 }),
    ];

    expect(sortEstimates(estimates, "total", "asc", customerNameById).map((e) => e.id)).toEqual([
      "2",
      "3",
      "1",
    ]);
    expect(sortEstimates(estimates, "total", "desc", customerNameById).map((e) => e.id)).toEqual([
      "1",
      "3",
      "2",
    ]);
  });

  it("sorts by customer name using the resolved lookup, not the raw customerId", () => {
    const estimates = [
      estimate({ id: "1", customerId: "cust1" }),
      estimate({ id: "2", customerId: "cust2" }),
    ];

    expect(
      sortEstimates(estimates, "customer", "asc", customerNameById).map((e) => e.id),
    ).toEqual(["2", "1"]); // Alfa Obras before Zeta Obras
  });

  it("sorts by issue date", () => {
    const estimates = [
      estimate({ id: "1", issueDate: "2026-07-20" }),
      estimate({ id: "2", issueDate: "2026-07-10" }),
    ];

    expect(
      sortEstimates(estimates, "issueDate", "asc", customerNameById).map((e) => e.id),
    ).toEqual(["2", "1"]);
  });

  it("does not mutate the original array", () => {
    const estimates = [estimate({ id: "1", total: 300 }), estimate({ id: "2", total: 100 })];
    const original = [...estimates];

    sortEstimates(estimates, "total", "asc", customerNameById);

    expect(estimates).toEqual(original);
  });
});
