import { describe, expect, it } from "vitest";
import { FALLBACK_SITE, getBrandingStyle, isSafeHexColor } from "./branding";

describe("branding", () => {
  it("accepts only full hex colors", () => {
    expect(isSafeHexColor("#E8500A")).toBe(true);
    expect(isSafeHexColor("red")).toBe(false);
    expect(isSafeHexColor("var(--x)")).toBe(false);
    expect(isSafeHexColor("#fff")).toBe(false);
  });

  it("applies only safe branding tokens", () => {
    const style = getBrandingStyle({
      ...FALLBACK_SITE,
      branding: {
        logoUrl: null,
        secondaryColor: null,
        tagline: null,
        aboutText: null,
        footerText: null,
        primaryColor: "#123456",
        accentColor: "url(javascript:alert(1))",
      },
    });

    expect(style).toMatchObject({ "--primary": "#123456" });
    expect(style).not.toHaveProperty("--accent");
  });
});
