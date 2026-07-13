import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FALLBACK_SITE_VIEW_MODEL } from "@/features/public-site/mappers/fallbacks";
import { SiteFooter } from "./SiteFooter";

const navLinks = [
  { href: "#servicos", label: "Serviços" },
  { href: "#contacto", label: "Contacto" },
];

describe("SiteFooter", () => {
  it("renders footer text without private company data", () => {
    render(<SiteFooter site={FALLBACK_SITE_VIEW_MODEL} navLinks={navLinks} />);

    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Contacto" })).toHaveAttribute(
      "href",
      "#contacto",
    );
    expect(screen.queryByText(/tax/i)).not.toBeInTheDocument();
    expect(
      screen.getByText(/Todos os direitos reservados/i),
    ).toBeInTheDocument();
  });
});
