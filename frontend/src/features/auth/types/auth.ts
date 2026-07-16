import { z } from "zod";

const nullableString = z.string().nullable();

export const authUserDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  email: z.string().email(),
  name: z.string(),
  role: z.string(),
  status: z.string(),
});

export const authCompanyDtoSchema = z.object({
  id: z.string(),
  name: z.string(),
  slug: z.string(),
  email: nullableString,
  country: nullableString,
  status: z.string(),
});

export const authBrandingDtoSchema = z
  .object({
    id: z.string(),
    companyId: z.string(),
    logoUrl: nullableString,
    primaryColor: nullableString,
    secondaryColor: nullableString,
    accentColor: nullableString,
    tagline: nullableString,
    aboutText: nullableString,
  })
  .nullable();

export const authSettingsDtoSchema = z
  .object({
    id: z.string(),
    companyId: z.string(),
    defaultCurrency: nullableString,
    defaultTaxRate: z.number().nullable(),
    estimateValidityDays: z.number().nullable(),
    locale: nullableString,
    timezone: nullableString,
  })
  .nullable();

export const authResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  user: authUserDtoSchema,
  company: authCompanyDtoSchema,
});

export const meResponseSchema = z.object({
  user: authUserDtoSchema,
  company: authCompanyDtoSchema,
  branding: authBrandingDtoSchema,
  settings: authSettingsDtoSchema,
});

export const loginFormSchema = z.object({
  email: z.string().email("Indique um email válido."),
  password: z.string().min(1, "Indique a password."),
});

export type AuthUserDto = z.infer<typeof authUserDtoSchema>;
export type AuthCompanyDto = z.infer<typeof authCompanyDtoSchema>;
export type AuthResponse = z.infer<typeof authResponseSchema>;
export type MeResponse = z.infer<typeof meResponseSchema>;
export type LoginFormValues = z.infer<typeof loginFormSchema>;
