import { describe, expect, it } from "vitest";
import { onboardingStatusSchema } from "./onboarding";

const validStatus = {
  companyCompleted: true,
  brandingCompleted: false,
  servicesCompleted: true,
  customerCompleted: false,
  estimateCompleted: false,
  teamCompleted: true,
};

describe("onboardingStatusSchema", () => {
  it("accepts the strict backend response", () => {
    expect(onboardingStatusSchema.parse(validStatus)).toEqual(validStatus);
  });

  it("rejects malformed responses and unexpected fields", () => {
    expect(() =>
      onboardingStatusSchema.parse({
        ...validStatus,
        estimateCompleted: "yes",
      }),
    ).toThrow();
    expect(() =>
      onboardingStatusSchema.parse({ ...validStatus, extra: true }),
    ).toThrow();
  });
});
