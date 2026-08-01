import { adminApiRequest } from "@/lib/api/admin-http-client";
import { ApiError } from "@/lib/api/errors";
import type { z } from "zod";
import {
  companiesPageSchema,
  companyAdminDetailResponseSchema,
  companyAdminSummarySchema,
  companyOnboardingResponseSchema,
  adminPasswordResetResponseSchema,
  inviteResponseSchema,
  ownerInviteResponseSchema,
  type CompaniesPage,
  type AdminPasswordResetResponse,
  type CompanyAdminDetailResponse,
  type CompanyAdminSummary,
  type CompanyOnboardingResponse,
  type CreateCompanyInput,
  type InviteOwnerInput,
  type InviteResponse,
  type OwnerInviteResponse,
} from "../types/admin";

function parseApiResponse<T>(schema: z.ZodType<T>, data: unknown): T {
  const result = schema.safeParse(data);
  if (!result.success) {
    throw new ApiError(
      "A resposta da API não tem o formato esperado.",
      0,
      null,
      "invalid-response",
    );
  }

  return result.data;
}

export async function listCompanies(
  accessToken: string,
  params: {
    search?: string;
    status?: "ACTIVE" | "INACTIVE" | "";
    page?: number;
    size?: number;
  } = {},
): Promise<CompaniesPage> {
  const query = new URLSearchParams();
  if (params.search) query.set("search", params.search);
  if (params.status) query.set("status", params.status);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 10));

  const response = await adminApiRequest<unknown>(
    `/admin/companies?${query.toString()}`,
    { accessToken },
  );
  return parseApiResponse(companiesPageSchema, response);
}

export async function createCompany(
  accessToken: string,
  values: CreateCompanyInput,
): Promise<CompanyOnboardingResponse> {
  const response = await adminApiRequest<unknown>("/admin/companies", {
    accessToken,
    method: "POST",
    body: JSON.stringify(values),
  });
  return parseApiResponse(companyOnboardingResponseSchema, response);
}

export async function getCompany(
  accessToken: string,
  companyId: string,
): Promise<CompanyAdminDetailResponse> {
  const response = await adminApiRequest<unknown>(
    `/admin/companies/${encodeURIComponent(companyId)}`,
    { accessToken },
  );
  return parseApiResponse(companyAdminDetailResponseSchema, response);
}

export async function updateCompanyStatus(
  accessToken: string,
  companyId: string,
  status: "ACTIVE" | "INACTIVE",
): Promise<CompanyAdminSummary> {
  const response = await adminApiRequest<unknown>(
    `/admin/companies/${encodeURIComponent(companyId)}/status`,
    {
      accessToken,
      method: "PATCH",
      body: JSON.stringify({ status }),
    },
  );
  return parseApiResponse(companyAdminSummarySchema, response);
}

export async function inviteOwner(
  accessToken: string,
  companyId: string,
  values: InviteOwnerInput,
): Promise<OwnerInviteResponse> {
  const response = await adminApiRequest<unknown>(
    `/admin/companies/${encodeURIComponent(companyId)}/owners`,
    {
      accessToken,
      method: "POST",
      body: JSON.stringify(values),
    },
  );
  return parseApiResponse(ownerInviteResponseSchema, response);
}

export async function reissueInvite(
  accessToken: string,
  companyId: string,
  ownerId: string,
): Promise<InviteResponse> {
  const response = await adminApiRequest<unknown>(
    `/admin/companies/${encodeURIComponent(companyId)}/owners/${encodeURIComponent(ownerId)}/invites`,
    {
      accessToken,
      method: "POST",
    },
  );
  return parseApiResponse(inviteResponseSchema, response);
}

export async function revokeInvite(
  accessToken: string,
  companyId: string,
  ownerId: string,
) {
  await adminApiRequest<void>(
    `/admin/companies/${encodeURIComponent(companyId)}/owners/${encodeURIComponent(ownerId)}/invite`,
    {
      accessToken,
      method: "DELETE",
    },
  );
}

export async function generateOwnerPasswordResetLink(
  accessToken: string,
  companyId: string,
  ownerId: string,
): Promise<AdminPasswordResetResponse> {
  const response = await adminApiRequest<unknown>(
    `/admin/companies/${encodeURIComponent(companyId)}/owners/${encodeURIComponent(ownerId)}/password-reset`,
    {
      accessToken,
      method: "POST",
    },
  );
  return parseApiResponse(adminPasswordResetResponseSchema, response);
}
