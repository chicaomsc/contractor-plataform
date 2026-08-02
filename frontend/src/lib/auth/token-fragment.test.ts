import { describe, expect, it } from "vitest";
import { readTokenFromHash } from "./token-fragment";

describe("readTokenFromHash", () => {
  it("reads a token from a leading-# hash", () => {
    expect(readTokenFromHash("#token=abc123")).toBe("abc123");
  });

  it("reads a token from a hash without the leading #", () => {
    expect(readTokenFromHash("token=abc123")).toBe("abc123");
  });

  it("trims whitespace", () => {
    expect(readTokenFromHash("#token=%20abc123%20")).toBe("abc123");
  });

  it("returns null for an empty hash", () => {
    expect(readTokenFromHash("")).toBeNull();
    expect(readTokenFromHash("#")).toBeNull();
  });

  it("returns null when the token param is absent", () => {
    expect(readTokenFromHash("#other=value")).toBeNull();
  });

  it("returns null for a blank token value", () => {
    expect(readTokenFromHash("#token=")).toBeNull();
  });
});
