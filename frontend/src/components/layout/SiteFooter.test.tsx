import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FALLBACK_SITE } from "@/features/public-site/branding";
import { SiteFooter } from "./SiteFooter";

describe("SiteFooter", () => {
  it("renders footer text without private company data", () => {
    render(<SiteFooter site={FALLBACK_SITE} />);

    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
    expect(screen.queryByText(/tax/i)).not.toBeInTheDocument();
    expect(
      screen.getByText(/Todos os direitos reservados/i),
    ).toBeInTheDocument();
  });
});
