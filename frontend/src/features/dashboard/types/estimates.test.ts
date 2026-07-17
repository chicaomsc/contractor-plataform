import { describe, expect, it } from "vitest";
import {
  estimateDtoSchema,
  estimateGeneralInfoSchema,
  estimateItemFormSchema,
  estimateSummaryDtoSchema,
  materialFormSchema,
} from "./estimates";

function estimateResponseFixture() {
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
  };
}

describe("estimate schemas", () => {
  it("parses a full EstimateResponse, including backend-calculated totals", () => {
    const result = estimateDtoSchema.parse(estimateResponseFixture());

    expect(result.total).toBe(184.5);
    expect(result.upfrontAmount).toBe(92.25);
    expect(result.status).toBe("DRAFT");
  });

  it("rejects an unknown status value", () => {
    expect(() =>
      estimateDtoSchema.parse({ ...estimateResponseFixture(), status: "UNKNOWN" }),
    ).toThrow();
  });

  it("parses an EstimateSummaryResponse without items/materials", () => {
    const summary = estimateSummaryDtoSchema.parse({
      id: "e1",
      companyId: "c1",
      customerId: "cust1",
      number: "ORC-2026-0001",
      title: "Pintura interior",
      status: "SENT",
      issueDate: "2026-07-16",
      validUntil: null,
      currency: "EUR",
      total: 184.5,
      upfrontAmount: 92.25,
      remainingAmount: 92.25,
      createdAt: "2026-07-16T10:00:00Z",
      updatedAt: "2026-07-16T10:00:00Z",
    });

    expect(summary.total).toBe(184.5);
    expect("items" in summary).toBe(false);
  });

  it("validates estimate item form input: quantity must be positive", () => {
    expect(() =>
      estimateItemFormSchema.parse({
        serviceId: null,
        description: "Pintura de parede",
        quantity: 0,
        unit: "M2",
        unitPrice: 10,
      }),
    ).toThrow();

    expect(
      estimateItemFormSchema.parse({
        serviceId: null,
        description: "Pintura de parede",
        quantity: "2.5",
        unit: "M2",
        unitPrice: "10",
      }),
    ).toMatchObject({ quantity: 2.5, unitPrice: 10 });
  });

  it("validates material form input the same way as item input", () => {
    expect(() =>
      materialFormSchema.parse({
        name: "",
        description: "",
        quantity: 1,
        unit: "UNIT",
        unitPrice: 5,
      }),
    ).toThrow();

    expect(
      materialFormSchema.parse({
        name: "Tinta branca",
        description: "",
        quantity: "3",
        unit: "UNIT",
        unitPrice: "12.50",
      }),
    ).toMatchObject({ name: "Tinta branca", description: null, quantity: 3 });
  });

  it("normalizes empty general-info fields to null", () => {
    const result = estimateGeneralInfoSchema.parse({
      title: "Pintura interior",
      description: "",
      validUntil: "",
      notes: "",
      terms: "",
    });

    expect(result).toMatchObject({
      description: null,
      validUntil: null,
      notes: null,
      terms: null,
    });
  });

  it("requires a title for general info", () => {
    expect(() =>
      estimateGeneralInfoSchema.parse({
        title: "",
        description: "",
        validUntil: "",
        notes: "",
        terms: "",
      }),
    ).toThrow();
  });
});
