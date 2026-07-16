export const dashboardQueryKeys = {
  all: ["dashboard"] as const,
  company: () => [...dashboardQueryKeys.all, "company"] as const,
  branding: () => [...dashboardQueryKeys.all, "branding"] as const,
  settings: () => [...dashboardQueryKeys.all, "settings"] as const,
  services: () => [...dashboardQueryKeys.all, "services"] as const,
  gallery: () => [...dashboardQueryKeys.all, "gallery"] as const,
};
