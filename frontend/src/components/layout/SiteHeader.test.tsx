import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FALLBACK_SITE } from "@/features/public-site/branding";
import { SiteHeader } from "./SiteHeader";

describe("SiteHeader", () => {
  it("renders a generic company name fallback", () => {
    render(<SiteHeader site={FALLBACK_SITE} />);

    expect(screen.getByRole("banner")).toBeInTheDocument();
    expect(screen.getByText("Contractor Platform")).toBeInTheDocument();
  });

  it("renders public phone when available", () => {
    render(
      <SiteHeader
        site={{ ...FALLBACK_SITE, publicPhone: "+351 910 000 000" }}
      />,
    );

    expect(screen.getByRole("link", { name: /telefonar/i })).toHaveAttribute(
      "href",
      "tel:+351910000000",
    );
  });
});
