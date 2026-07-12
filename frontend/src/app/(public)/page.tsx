import { TechnicalPlaceholder } from "@/components/feedback/TechnicalPlaceholder";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { FALLBACK_SITE } from "@/features/public-site/branding";
import { getPublicSite } from "@/lib/api/public-site";
import { getPublicEnv } from "@/lib/env/public-env";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  let site = FALLBACK_SITE;
  let usingFallback = false;

  try {
    const env = getPublicEnv();
    site = await getPublicSite(env.NEXT_PUBLIC_COMPANY_SLUG);
  } catch {
    usingFallback = true;
  }

  return (
    <PublicLayout site={site} usingFallback={usingFallback}>
      <TechnicalPlaceholder />
    </PublicLayout>
  );
}
