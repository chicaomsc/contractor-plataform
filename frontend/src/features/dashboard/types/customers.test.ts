import { describe, expect, it } from "vitest";
import { customerDtoSchema, quickCustomerFormSchema } from "./customers";

describe("customer schemas", () => {
  it("parses a full CustomerResponse", () => {
    const result = customerDtoSchema.parse({
      id: "cust1",
      companyId: "c1",
      name: "Jane Doe",
      email: "jane@example.com",
      phone: "912345678",
      taxNumber: null,
      address: null,
      notes: null,
      active: true,
      createdAt: "2026-07-16T10:00:00Z",
      updatedAt: "2026-07-16T10:00:00Z",
    });

    expect(result.name).toBe("Jane Doe");
    expect(result.active).toBe(true);
  });

  it("requires at least one of email or phone (mirrors CustomerService#requireContactInfo)", () => {
    expect(() =>
      quickCustomerFormSchema.parse({
        name: "Jane Doe",
        email: "",
        phone: "",
        taxNumber: "",
        address: { street: "", city: "", postalCode: "", region: "", country: "" },
        notes: "",
      }),
    ).toThrow();
  });

  it("accepts a customer with only a phone number", () => {
    const result = quickCustomerFormSchema.parse({
      name: "Jane Doe",
      email: "",
      phone: "912345678",
      taxNumber: "",
      address: { street: "", city: "", postalCode: "", region: "", country: "" },
      notes: "",
    });

    expect(result.phone).toBe("912345678");
    expect(result.email).toBeNull();
  });

  it("accepts a customer with only an email", () => {
    const result = quickCustomerFormSchema.parse({
      name: "Jane Doe",
      email: "jane@example.com",
      phone: "",
      taxNumber: "",
      address: { street: "", city: "", postalCode: "", region: "", country: "" },
      notes: "",
    });

    expect(result.email).toBe("jane@example.com");
    expect(result.phone).toBeNull();
  });

  it("rejects an invalid email format", () => {
    expect(() =>
      quickCustomerFormSchema.parse({
        name: "Jane Doe",
        email: "not-an-email",
        phone: "",
        taxNumber: "",
        address: { street: "", city: "", postalCode: "", region: "", country: "" },
        notes: "",
      }),
    ).toThrow();
  });
});
