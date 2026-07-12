import { PublicSiteIntegrationShell } from "@/features/public-site";
import { getPublicEnv } from "@/lib/env/public-env";

export const dynamic = "force-dynamic";

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
