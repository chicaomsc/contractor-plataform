import React from "react";
import { render, screen } from "@testing-library/react";
import type { UseQueryResult } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/errors";
import type {
  PublicGalleryItemViewModel,
  PublicServiceViewModel,
  PublicSiteViewModel,
} from "../types/view-model";
import { PublicSiteIntegrationPreview } from "./PublicSiteIntegrationPreview";

const site: PublicSiteViewModel = {
  slug: "empresa-teste",
  name: "Empresa Teste Lda",
  displayName: "Empresa Teste",
  publicPhone: null,
  whatsapp: null,
  website: null,
  locationLabel: "Porto, PT",
  branding: {
    logoUrl: null,
    primaryColor: "#123456",
    accentColor: null,
    hasValidPrimaryColor: true,
    hasValidAccentColor: false,
  },
  footerText: null,
};

const services: PublicServiceViewModel[] = [
  {
    id: "service-1",
    name: "Serviço Público",
    slug: "servico-publico",
    summary: "Resumo",
    displayOrder: 0,
  },
];

const gallery: PublicGalleryItemViewModel[] = [
  {
    id: "gallery-1",
    title: "Obra Pública",
    description: null,
    displayOrder: 0,
    featured: true,
    hasBeforeImage: true,
    hasAfterImage: true,
    hasCompleteBeforeAfterPair: true,
  },
];

function query<T>(partial: Partial<UseQueryResult<T, Error>>) {
  return partial as UseQueryResult<T, Error>;
}

describe("PublicSiteIntegrationPreview", () => {
  it("renders complete technical preview data", () => {
    render(
      <PublicSiteIntegrationPreview
        companySlug="empresa-teste"
        siteQuery={query({ isSuccess: true, data: site })}
        servicesQuery={query({ isSuccess: true, data: services })}
        galleryQuery={query({ isSuccess: true, data: gallery })}
      />,
    );

    expect(screen.getByText("Preview técnico temporário")).toBeInTheDocument();
    expect(screen.getByText("Empresa Teste")).toBeInTheDocument();
    expect(screen.getByText("Serviço Público")).toBeInTheDocument();
    expect(screen.getByText("Obra Pública")).toBeInTheDocument();
    expect(
      screen.getByText(/Pares before\/after completos: 1/i),
    ).toBeInTheDocument();
  });

  it("renders blocking error for site not found", () => {
    render(
      <PublicSiteIntegrationPreview
        companySlug="missing"
        siteQuery={query({
          isError: true,
          error: new ApiError("Not found", 404),
        })}
        servicesQuery={query({})}
        galleryQuery={query({})}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Site público indisponível",
    );
    expect(
      screen.getByText(/Não encontrámos este site público/i),
    ).toBeInTheDocument();
  });

  it("renders partial failure while preserving loaded site", () => {
    render(
      <PublicSiteIntegrationPreview
        companySlug="empresa-teste"
        siteQuery={query({ isSuccess: true, data: site })}
        servicesQuery={query({
          isError: true,
          error: new ApiError("Serviços indisponíveis", 0, null, "network"),
        })}
        galleryQuery={query({ isSuccess: true, data: [] })}
      />,
    );

    expect(screen.getByText("Empresa Teste")).toBeInTheDocument();
    expect(screen.getByText("Serviços indisponíveis")).toBeInTheDocument();
    expect(screen.getByText(/ainda não tem itens ativos/i)).toBeInTheDocument();
  });

  it("renders empty states for empty collections and missing slug", () => {
    const { rerender } = render(
      <PublicSiteIntegrationPreview
        companySlug="empresa-teste"
        siteQuery={query({ isSuccess: true, data: site })}
        servicesQuery={query({ isSuccess: true, data: [] })}
        galleryQuery={query({ isSuccess: true, data: [] })}
      />,
    );

    expect(
      screen.getByText(/ainda não tem serviços ativos/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/ainda não tem itens ativos/i)).toBeInTheDocument();

    rerender(
      <PublicSiteIntegrationPreview
        companySlug={null}
        siteQuery={query({})}
        servicesQuery={query({})}
        galleryQuery={query({})}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Slug público não configurado",
    );
  });
});
