import { z } from "zod";
import { companyStatusSchema, userRoleSchema, userStatusSchema } from "@/features/auth/types/auth";

export const companyAdminSummarySchema = z.object({
  id: z.string(),
  name: z.string(),
  slug: z.string(),
  status: companyStatusSchema,
  ownerEmail: z.string().email().nullable(),
  createdAt: z.string(),
});

export const companyAdminDetailSchema = z.object({
  id: z.string(),
  name: z.string(),
  slug: z.string(),
  email: z.string().nullable(),
  country: z.string().nullable(),
  tradeName: z.string().nullable(),
  status: companyStatusSchema,
  createdAt: z.string(),
});

export const ownerAdminSchema = z.object({
  id: z.string(),
  email: z.string().email(),
  name: z.string(),
  role: userRoleSchema,
  status: userStatusSchema,
  createdAt: z.string(),
});

export const companyAdminDetailResponseSchema = z.object({
  company: companyAdminDetailSchema,
  owners: z.array(ownerAdminSchema),
});

export const inviteSchema = z.object({
  token: z.string(),
  expiresAt: z.string(),
});

export const companyOnboardingResponseSchema = z.object({
  company: z.object({
    id: z.string(),
    name: z.string(),
    slug: z.string(),
    country: z.string(),
    status: companyStatusSchema,
  }),
  owner: z.object({
    id: z.string(),
    email: z.string().email(),
    name: z.string(),
    status: userStatusSchema,
  }),
  invite: inviteSchema,
});

export const ownerInviteResponseSchema = z.object({
  owner: z.object({
    id: z.string(),
    email: z.string().email(),
    name: z.string(),
    status: userStatusSchema,
  }),
  invite: inviteSchema,
});

export const inviteResponseSchema = z.object({
  invite: inviteSchema,
});

export const adminPasswordResetResponseSchema = z.object({
  resetLink: z.string(),
  expiresAt: z.string(),
});

export const companiesPageSchema = z.object({
  content: z.array(companyAdminSummarySchema),
  totalElements: z.number(),
  totalPages: z.number(),
  size: z.number(),
  number: z.number(),
  first: z.boolean().optional(),
  last: z.boolean().optional(),
});

const emptyToUndefined = (value: unknown) => value === "" ? undefined : value;

export const createCompanySchema = z.object({
  companyName: z.string().min(2, "Indique pelo menos 2 caracteres.").max(100),
  slug: z.preprocess(
    emptyToUndefined,
    z.string().regex(/^[a-z0-9]+(-[a-z0-9]+)*$/, "Use minúsculas, números e hífens.").optional(),
  ),
  country: z.string().min(2, "Use ISO alpha-2.").max(2, "Use ISO alpha-2."),
  tradeName: z.preprocess(emptyToUndefined, z.string().optional()),
  ownerName: z.string().min(1, "Indique o responsável."),
  ownerEmail: z.string().email("Indique um email válido."),
});

export const inviteOwnerSchema = z.object({
  ownerName: z.string().min(1, "Indique o responsável."),
  ownerEmail: z.string().email("Indique um email válido."),
});

export type CompanyAdminSummary = z.infer<typeof companyAdminSummarySchema>;
export type CompanyAdminDetailResponse = z.infer<typeof companyAdminDetailResponseSchema>;
export type CompanyOnboardingResponse = z.infer<typeof companyOnboardingResponseSchema>;
export type OwnerInviteResponse = z.infer<typeof ownerInviteResponseSchema>;
export type InviteResponse = z.infer<typeof inviteResponseSchema>;
export type AdminPasswordResetResponse = z.infer<typeof adminPasswordResetResponseSchema>;
export type CompaniesPage = z.infer<typeof companiesPageSchema>;
export type CreateCompanyInput = z.infer<typeof createCompanySchema>;
export type InviteOwnerInput = z.infer<typeof inviteOwnerSchema>;
