import type { Metadata } from "next";
import { PublicSiteIntegrationShell } from "@/features/public-site";
import { getPublicEnv } from "@/lib/env/public-env";

export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const env = getPublicEnv();
  const canonical = env.NEXT_PUBLIC_SITE_URL ?? undefined;

  return {
    title: "Contractor Platform",
    description: "Presença digital pública para prestadores de serviço.",
    alternates: canonical ? { canonical } : undefined,
  };
}

export default async function HomePage() {
  return <PublicSiteIntegrationShell />;
}
