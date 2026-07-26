import { getPublicEnv } from "@/lib/env/public-env";

type BuildTenantLandingUrlOptions = {
  siteUrl?: string;
  platformBaseDomain?: string | null;
  currentOrigin?: string;
};

function getCurrentOrigin() {
  return typeof window === "undefined" ? "http://localhost" : window.location.origin;
}

export function buildTenantLandingUrl(
  slug: string,
  options: BuildTenantLandingUrlOptions = {},
): string {
  let env: ReturnType<typeof getPublicEnv> | null = null;

  try {
    env = getPublicEnv();
  } catch {
    env = null;
  }

  const baseUrl =
    options.siteUrl ??
    env?.NEXT_PUBLIC_SITE_URL ??
    options.currentOrigin ??
    getCurrentOrigin();
  const url = new URL(baseUrl);
  const platformBaseDomain =
    options.platformBaseDomain ?? env?.NEXT_PUBLIC_PLATFORM_BASE_DOMAIN;
  const trimmedBaseDomain = platformBaseDomain?.trim();

  if (trimmedBaseDomain) {
    url.hostname = `${slug}.${trimmedBaseDomain}`;
  }

  return url.toString();
}
