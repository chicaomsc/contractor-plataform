export const platformAdminQueryKeys = {
  all: ["platform-admin"] as const,
  companies: (params: unknown) =>
    [...platformAdminQueryKeys.all, "companies", params] as const,
  company: (companyId: string) =>
    [...platformAdminQueryKeys.all, "company", companyId] as const,
};
