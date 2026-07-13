import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { FALLBACK_SITE_VIEW_MODEL } from "@/features/public-site/mappers/fallbacks";
import { SiteHeader } from "./SiteHeader";

const navLinks = [
  { href: "#servicos", label: "Serviços" },
  { href: "#contacto", label: "Contacto" },
];

describe("SiteHeader", () => {
  it("renders a generic company name fallback", () => {
    render(<SiteHeader site={FALLBACK_SITE_VIEW_MODEL} navLinks={navLinks} />);

    expect(screen.getByRole("banner")).toBeInTheDocument();
    expect(screen.getByText("Contractor Platform")).toBeInTheDocument();
    expect(
      screen.getAllByRole("link", { name: "Serviços" })[0],
    ).toHaveAttribute("href", "#servicos");
  });

  it("renders public phone when available", () => {
    render(
      <SiteHeader
        site={{ ...FALLBACK_SITE_VIEW_MODEL, publicPhone: "+351 910 000 000" }}
        navLinks={navLinks}
      />,
    );

    expect(screen.getByRole("link", { name: /telefonar/i })).toHaveAttribute(
      "href",
      "tel:+351910000000",
    );
  });

  it("opens and closes the mobile menu with accessible controls", async () => {
    const user = userEvent.setup();
    render(<SiteHeader site={FALLBACK_SITE_VIEW_MODEL} navLinks={navLinks} />);

    await user.click(screen.getByRole("button", { name: /abrir menu/i }));
    expect(
      screen.getByRole("dialog", { name: /menu principal/i }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /fechar menu/i }));
    expect(
      screen.queryByRole("dialog", { name: /menu principal/i }),
    ).not.toBeInTheDocument();
  });
});
