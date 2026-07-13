import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PublicLandingPage } from "./PublicLandingPage";
import type {
  PublicGalleryItemViewModel,
  PublicServiceViewModel,
  PublicSiteViewModel,
} from "../types/view-model";

const site: PublicSiteViewModel = {
  slug: "tenant-demo",
  name: "Tenant Demo",
  displayName: "Obras Demo",
  publicPhone: "+351 210 000 000",
  whatsapp: "+351 910 000 000",
  website: "https://example.test",
  locationLabel: "Lisboa, Portugal",
  tagline: "Pintura e remodelação com obra organizada",
  aboutText: "Equipa focada em preparação, acabamento e entrega cuidada.",
  footerText: "Texto público de rodapé.",
  branding: {
    primaryColor: "#d95f18",
    accentColor: "#26221f",
    logoUrl: null,
    hasValidPrimaryColor: true,
    hasValidAccentColor: true,
  },
};

const services: PublicServiceViewModel[] = [
  {
    id: "service-2",
    name: "Remodelação interior",
    slug: "remodelacao-interior",
    summary: "Preparação e acabamento de espaços interiores.",
    displayOrder: 2,
  },
  {
    id: "service-1",
    name: "Pintura interior",
    slug: "pintura-interior",
    summary: "Paredes, tetos e acabamentos interiores.",
    displayOrder: 1,
  },
];

const gallery: PublicGalleryItemViewModel[] = [
  {
    id: "gallery-1",
    title: "Apartamento renovado",
    description: "Intervenção com preparação e pintura.",
    beforeImageUrl: "/fixtures/before.jpg",
    afterImageUrl: "/fixtures/after.jpg",
    beforeAlt: "Apartamento antes da intervenção",
    afterAlt: "Apartamento depois da intervenção",
    displayOrder: 1,
    featured: true,
    hasBeforeImage: true,
    hasAfterImage: true,
    hasCompleteBeforeAfterPair: true,
  },
];

describe("PublicLandingPage", () => {
  it("renders hero with configured copy and image", () => {
    render(
      <PublicLandingPage site={site} services={services} gallery={gallery} />,
    );

    expect(
      screen.getByRole("heading", {
        level: 1,
        name: /pintura e remodelação com obra organizada/i,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getAllByAltText(/apartamento depois da intervenção/i).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getByRole("link", { name: /pedir orçamento/i }),
    ).toHaveAttribute(
      "href",
      expect.stringContaining("https://wa.me/351910000000"),
    );
  });

  it("renders a professional hero fallback without an image", () => {
    render(<PublicLandingPage site={site} services={services} gallery={[]} />);

    expect(
      screen.getByText(/fotografia pública ainda não configurada/i),
    ).toBeInTheDocument();
  });

  it("keeps services in the received view-model order", () => {
    render(
      <PublicLandingPage site={site} services={services} gallery={gallery} />,
    );

    const serviceItems = screen.getAllByRole("listitem");
    expect(serviceItems.at(0)).toHaveTextContent(/remodelação interior/i);
    expect(serviceItems.at(1)).toHaveTextContent(/pintura interior/i);
  });

  it("renders an empty state when services are absent", () => {
    render(<PublicLandingPage site={site} services={[]} gallery={gallery} />);

    expect(screen.getByRole("status")).toHaveTextContent(
      /serviços públicos ainda não configurados/i,
    );
  });

  it("renders the gallery comparison when a complete pair exists", () => {
    render(
      <PublicLandingPage site={site} services={services} gallery={gallery} />,
    );

    expect(
      screen.getByRole("region", { name: /antes e depois/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("slider", { name: /baseline/i }),
    ).toBeInTheDocument();
  });

  it("renders a partial gallery fallback without a comparison slider", () => {
    render(
      <PublicLandingPage
        site={site}
        services={services}
        gallery={[
          {
            ...gallery[0],
            afterImageUrl: null,
            afterAlt: null,
            hasAfterImage: false,
            hasCompleteBeforeAfterPair: false,
          },
        ]}
      />,
    );

    expect(
      screen.getByText(
        /a galeria ainda não tem pares antes\/depois completos/i,
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("slider", { name: /baseline/i }),
    ).not.toBeInTheDocument();
  });

  it("omits optional area and about sections when public data is absent", () => {
    render(
      <PublicLandingPage
        site={{ ...site, locationLabel: null, aboutText: null }}
        services={services}
        gallery={[]}
      />,
    );

    expect(
      screen.queryByRole("region", { name: /área de atuação/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: /sobre/i }),
    ).not.toBeInTheDocument();
  });

  it("renders contact with WhatsApp and without WhatsApp", () => {
    const { rerender } = render(
      <PublicLandingPage site={site} services={services} gallery={gallery} />,
    );

    expect(
      screen.getAllByRole("link", { name: /whatsapp/i }).length,
    ).toBeGreaterThan(0);

    rerender(
      <PublicLandingPage
        site={{ ...site, whatsapp: null, publicPhone: "+351 210 000 000" }}
        services={services}
        gallery={gallery}
      />,
    );

    expect(screen.getByRole("link", { name: /telefonar/i })).toHaveAttribute(
      "href",
      "tel:+351210000000",
    );
  });

  it("renders partial error states without blocking the page", () => {
    render(
      <PublicLandingPage
        site={site}
        services={services}
        gallery={gallery}
        servicesError
        galleryError
      />,
    );

    expect(screen.getAllByRole("status").length).toBeGreaterThanOrEqual(2);
    expect(screen.getByRole("heading", { level: 1 })).toBeInTheDocument();
  });

  it("does not render tenant-specific hardcoded content", () => {
    const { container } = render(
      <PublicLandingPage site={site} services={services} gallery={gallery} />,
    );

    expect(container).not.toHaveTextContent(/jr pinturas|jr-pinturas/i);
  });
});
