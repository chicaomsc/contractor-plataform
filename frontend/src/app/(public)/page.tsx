import type { Metadata } from "next";
import { PublicSiteIntegrationShell } from "@/features/public-site";
import { fetchPublicSite } from "@/features/public-site/api/public-site-api";
import { mapPublicSiteDto } from "@/features/public-site/mappers/public-site";
import { getHeroHeadline } from "@/features/public-site/utils/content";
import { getPublicEnv } from "@/lib/env/public-env";

export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  try {
    const env = getPublicEnv();
    const dto = await fetchPublicSite(env.NEXT_PUBLIC_COMPANY_SLUG);
    const site = mapPublicSiteDto(dto);
    const title = site.displayName;
    const description = getHeroHeadline(site);
    const canonical = env.NEXT_PUBLIC_SITE_URL ?? undefined;

    return {
      title,
      description,
      alternates: canonical ? { canonical } : undefined,
      openGraph: {
        title,
        description,
        type: "website",
        locale: "pt_PT",
        siteName: title,
        url: canonical,
        images: site.branding.logoUrl ? [{ url: site.branding.logoUrl }] : [],
      },
    };
  } catch {
    return {
      title: "Contractor Platform",
      description: "Presença digital pública para prestadores de serviço.",
    };
  }
}

export default async function HomePage() {
  let companySlug: string | null = null;

  try {
    const env = getPublicEnv();
    companySlug = env.NEXT_PUBLIC_COMPANY_SLUG;
  } catch {
    companySlug = null;
  }

  return <PublicSiteIntegrationShell companySlug={companySlug} />;
}
