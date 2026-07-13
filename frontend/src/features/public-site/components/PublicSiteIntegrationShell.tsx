"use client";

import { PublicLayout } from "@/components/layout/PublicLayout";
import { ApiError } from "@/lib/api/errors";
import { FALLBACK_SITE_VIEW_MODEL } from "../mappers/fallbacks";
import { getBrandingStyle } from "../mappers/branding";
import {
  usePublicGallery,
  usePublicServices,
  usePublicSite,
} from "../hooks/public-site-hooks";
import { LandingBlockingError, LandingLoadingState } from "./LandingStates";
import { PublicLandingPage } from "./PublicLandingPage";
import type { NavLink } from "./landing-types";

type PublicSiteIntegrationShellProps = {
  companySlug: string | null;
};

export const publicLandingNavLinks: NavLink[] = [
  { label: "Serviços", href: "#servicos" },
  { label: "Trabalhos", href: "#trabalhos" },
  { label: "Como trabalhamos", href: "#como-trabalhamos" },
  { label: "Sobre", href: "#sobre" },
  { label: "Contacto", href: "#contacto" },
];

function getSiteErrorMessage(error: Error | null) {
  if (error instanceof ApiError && error.status === 404) {
    return "Não encontrámos esta empresa. Verifique o endereço configurado.";
  }

  if (error instanceof ApiError) {
    return error.message;
  }

  return "Não foi possível carregar os dados públicos da empresa.";
}

export function PublicSiteIntegrationShell({
  companySlug,
}: PublicSiteIntegrationShellProps) {
  const siteQuery = usePublicSite(companySlug);
  const siteLoaded = siteQuery.isSuccess;
  const servicesQuery = usePublicServices(companySlug, siteLoaded);
  const galleryQuery = usePublicGallery(companySlug, siteLoaded);
  const site = siteQuery.data ?? FALLBACK_SITE_VIEW_MODEL;

  if (!companySlug) {
    return (
      <PublicLayout
        site={site}
        navLinks={publicLandingNavLinks}
        style={getBrandingStyle(site)}
      >
        <LandingBlockingError
          title="Site público não configurado"
          message="Configure NEXT_PUBLIC_COMPANY_SLUG para carregar a landing pública."
        />
      </PublicLayout>
    );
  }

  if (siteQuery.isLoading) {
    return (
      <PublicLayout
        site={site}
        navLinks={publicLandingNavLinks}
        style={getBrandingStyle(site)}
      >
        <LandingLoadingState />
      </PublicLayout>
    );
  }

  if (siteQuery.isError || !siteQuery.data) {
    return (
      <PublicLayout
        site={site}
        navLinks={publicLandingNavLinks}
        style={getBrandingStyle(site)}
      >
        <LandingBlockingError
          title="Site público indisponível"
          message={getSiteErrorMessage(siteQuery.error)}
        />
      </PublicLayout>
    );
  }

  return (
    <PublicLayout
      site={site}
      navLinks={publicLandingNavLinks}
      style={getBrandingStyle(site)}
    >
      <PublicLandingPage
        site={site}
        services={servicesQuery.data ?? []}
        gallery={galleryQuery.data ?? []}
        servicesError={servicesQuery.isError}
        galleryError={galleryQuery.isError}
      />
    </PublicLayout>
  );
}
