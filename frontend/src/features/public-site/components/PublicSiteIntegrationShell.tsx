"use client";

import { PublicLayout } from "@/components/layout/PublicLayout";
import { FALLBACK_SITE_VIEW_MODEL } from "../mappers/fallbacks";
import { getBrandingStyle } from "../mappers/branding";
import {
  usePublicGallery,
  usePublicServices,
  usePublicSite,
} from "../hooks/public-site-hooks";
import { PublicSiteIntegrationPreview } from "./PublicSiteIntegrationPreview";

type PublicSiteIntegrationShellProps = {
  companySlug: string | null;
};

export function PublicSiteIntegrationShell({
  companySlug,
}: PublicSiteIntegrationShellProps) {
  const siteQuery = usePublicSite(companySlug);
  const siteLoaded = siteQuery.isSuccess;
  const servicesQuery = usePublicServices(companySlug, siteLoaded);
  const galleryQuery = usePublicGallery(companySlug, siteLoaded);
  const site = siteQuery.data ?? FALLBACK_SITE_VIEW_MODEL;

  return (
    <PublicLayout site={site} style={getBrandingStyle(site)}>
      <PublicSiteIntegrationPreview
        companySlug={companySlug}
        siteQuery={siteQuery}
        servicesQuery={servicesQuery}
        galleryQuery={galleryQuery}
      />
    </PublicLayout>
  );
}
