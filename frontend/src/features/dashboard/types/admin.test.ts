import { describe, expect, it } from "vitest";
import {
  updateBrandingSchema,
  updateCompanySchema,
  updateSettingsSchema,
} from "./admin";

describe("dashboard admin schemas", () => {
  it("normalizes empty company optional fields to null", () => {
    const result = updateCompanySchema.parse({
      name: "Acme Obras",
      tradeName: "",
      email: "",
      phone: "",
      whatsapp: "",
      website: "",
      taxNumber: "",
      country: "",
      address: {
        street: "",
        city: "",
        postalCode: "",
        region: "",
        country: "",
      },
    });

    expect(result).toMatchObject({
      tradeName: null,
      email: null,
      address: {
        city: null,
      },
    });
  });

  it("validates branding hex colours and allows absent optional text", () => {
    expect(() =>
      updateBrandingSchema.parse({
        primaryColor: "blue",
        secondaryColor: "",
        accentColor: "",
        tagline: "",
        aboutText: "",
        footerText: "",
        quotationPrefix: "",
        signatureName: "",
      }),
    ).toThrow();

    expect(
      updateBrandingSchema.parse({
        primaryColor: "#1E40AF",
        secondaryColor: "",
        accentColor: "",
        tagline: "",
        aboutText: "",
        footerText: "",
        quotationPrefix: "",
        signatureName: "",
      }),
    ).toMatchObject({
      primaryColor: "#1E40AF",
      secondaryColor: null,
      tagline: null,
    });
  });

  it("normalizes empty settings fields and coerces numeric values", () => {
    const result = updateSettingsSchema.parse({
      defaultCurrency: "EUR",
      defaultTaxRate: "23",
      estimateValidityDays: "15",
      estimateFooterText: "",
      locale: "",
      timezone: "",
      dateFormat: "",
      numberFormat: "",
    });

    expect(result).toMatchObject({
      defaultCurrency: "EUR",
      defaultTaxRate: 23,
      estimateValidityDays: 15,
      estimateFooterText: null,
      locale: null,
    });
  });
});
