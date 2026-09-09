import { describe, expect, it } from "vitest";
import {
  getReadableTextColor,
  isValidHexColor,
  normalizeHexColor,
  safeHexOrFallback,
} from "./colors";

describe("dashboard color utils", () => {
  it("validates only full HEX colors", () => {
    expect(isValidHexColor("#1E40AF")).toBe(true);
    expect(isValidHexColor("#1e40af")).toBe(true);
    expect(isValidHexColor("#FFF")).toBe(false);
    expect(isValidHexColor("blue")).toBe(false);
    expect(isValidHexColor("#XYZXYZ")).toBe(false);
  });

  it("normalizes valid HEX colors to uppercase", () => {
    expect(normalizeHexColor("#1e40af")).toBe("#1E40AF");
  });

  it("returns a fallback for unsafe colors", () => {
    expect(safeHexOrFallback("#0f766e", "#111111")).toBe("#0F766E");
    expect(safeHexOrFallback("#0F", "#111111")).toBe("#111111");
    expect(safeHexOrFallback("var(--primary)", "#111111")).toBe("#111111");
  });

  it("chooses readable text for light and dark colors", () => {
    expect(getReadableTextColor("#FFFFFF")).toBe("#111827");
    expect(getReadableTextColor("#111827")).toBe("#FFFFFF");
  });
});
