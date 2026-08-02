import { afterEach, describe, expect, it, vi } from "vitest";
import { buildInviteLink } from "./invite-links";

describe("buildInviteLink", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("uses a URL fragment, never a query string (SEC-AUTH-07)", () => {
    const link = buildInviteLink("raw-token");

    expect(link).toContain("/invite#token=raw-token");
    expect(link).not.toContain("?token=");
  });

  it("builds an absolute link using window.location.origin when available", () => {
    const link = buildInviteLink("raw-token");

    expect(link).toBe(`${window.location.origin}/invite#token=raw-token`);
  });

  it("encodes the token", () => {
    const link = buildInviteLink("a b/c");

    expect(link).toContain(`#token=${encodeURIComponent("a b/c")}`);
  });
});
