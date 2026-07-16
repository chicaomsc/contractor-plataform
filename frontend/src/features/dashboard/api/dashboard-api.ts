import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  brandingDtoSchema,
  companyDtoSchema,
  galleryItemsDtoSchema,
  servicesDtoSchema,
  settingsDtoSchema,
  type BrandingDto,
  type CompanyDto,
  type GalleryDto,
  type ServiceDto,
  type ServiceFormInput,
  type SettingsDto,
  type UpdateBrandingInput,
  type UpdateCompanyInput,
  type UpdateSettingsInput,
} from "../types/admin";

export async function fetchCompany(accessToken: string): Promise<CompanyDto> {
  const response = await adminApiRequest<unknown>("/company/me", {
    accessToken,
  });
  return companyDtoSchema.parse(response);
}

export async function updateCompany(
  accessToken: string,
  payload: UpdateCompanyInput,
): Promise<CompanyDto> {
  const response = await adminApiRequest<unknown>("/company/me", {
    method: "PUT",
    accessToken,
    body: JSON.stringify(payload),
  });
  return companyDtoSchema.parse(response);
}

export async function fetchBranding(accessToken: string): Promise<BrandingDto> {
  const response = await adminApiRequest<unknown>("/branding/me", {
    accessToken,
  });
  return brandingDtoSchema.parse(response);
}

export async function updateBranding(
  accessToken: string,
  payload: UpdateBrandingInput,
): Promise<BrandingDto> {
  const response = await adminApiRequest<unknown>("/branding/me", {
    method: "PUT",
    accessToken,
    body: JSON.stringify(payload),
  });
  return brandingDtoSchema.parse(response);
}

export async function fetchSettings(accessToken: string): Promise<SettingsDto> {
  const response = await adminApiRequest<unknown>("/settings/me", {
    accessToken,
  });
  return settingsDtoSchema.parse(response);
}

export async function updateSettings(
  accessToken: string,
  payload: UpdateSettingsInput,
): Promise<SettingsDto> {
  const response = await adminApiRequest<unknown>("/settings/me", {
    method: "PUT",
    accessToken,
    body: JSON.stringify(payload),
  });
  return settingsDtoSchema.parse(response);
}

export async function fetchServices(accessToken: string): Promise<ServiceDto[]> {
  const response = await adminApiRequest<unknown>("/services", {
    accessToken,
  });
  return servicesDtoSchema.parse(response);
}

export async function createService(
  accessToken: string,
  payload: ServiceFormInput,
): Promise<ServiceDto> {
  const response = await adminApiRequest<unknown>("/services", {
    method: "POST",
    accessToken,
    body: JSON.stringify(payload),
  });
  return servicesDtoSchema.element.parse(response);
}

export async function updateService(
  accessToken: string,
  serviceId: string,
  payload: ServiceFormInput,
): Promise<ServiceDto> {
  const response = await adminApiRequest<unknown>(`/services/${serviceId}`, {
    method: "PUT",
    accessToken,
    body: JSON.stringify(payload),
  });
  return servicesDtoSchema.element.parse(response);
}

export async function deleteService(
  accessToken: string,
  serviceId: string,
): Promise<void> {
  await adminApiRequest<void>(`/services/${serviceId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function reorderService(
  accessToken: string,
  serviceId: string,
  displayOrder: number,
): Promise<ServiceDto> {
  const response = await adminApiRequest<unknown>(
    `/services/${serviceId}/reorder`,
    {
      method: "PATCH",
      accessToken,
      body: JSON.stringify({ displayOrder }),
    },
  );
  return servicesDtoSchema.element.parse(response);
}

export async function fetchGallery(accessToken: string): Promise<GalleryDto[]> {
  const response = await adminApiRequest<unknown>("/gallery", {
    accessToken,
  });
  return galleryItemsDtoSchema.parse(response);
}
