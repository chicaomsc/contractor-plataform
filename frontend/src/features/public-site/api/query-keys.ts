export const publicSiteQueryKeys = {
  all: ["public-site"] as const,
  site: (companySlug: string) =>
    [...publicSiteQueryKeys.all, "site", companySlug] as const,
  services: (companySlug: string) =>
    [...publicSiteQueryKeys.all, "services", companySlug] as const,
  gallery: (companySlug: string) =>
    [...publicSiteQueryKeys.all, "gallery", companySlug] as const,
};
