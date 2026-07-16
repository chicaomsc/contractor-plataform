import { describe, expect, it } from "vitest";
import { emptyToNull, formatDateTime, latestIsoDate } from "./forms";

describe("dashboard form utilities", () => {
  it("returns null for blank text values", () => {
    expect(emptyToNull("")).toBeNull();
    expect(emptyToNull("   ")).toBeNull();
    expect(emptyToNull(" JR ")).toBe("JR");
  });

  it("finds the latest valid ISO date", () => {
    expect(
      latestIsoDate([
        "2026-07-15T10:00:00.000Z",
        null,
        "invalid",
        "2026-07-16T10:00:00.000Z",
      ]),
    ).toBe("2026-07-16T10:00:00.000Z");
  });

  it("formats absent dates with the dashboard empty state label", () => {
    expect(formatDateTime(null)).toBe("Sem atualização registada");
  });
});
