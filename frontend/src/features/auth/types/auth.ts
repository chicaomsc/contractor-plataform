import { z } from "zod";

const nullableString = z.string().nullable();

export const userRoleSchema = z.enum(["OWNER", "SUPER_ADMIN"]);
export const userStatusSchema = z.enum(["ACTIVE", "INACTIVE", "PENDING"]);
export const companyStatusSchema = z.enum(["ACTIVE", "INACTIVE"]);

export const authUserDtoSchema = z.object({
  id: z.string(),
  companyId: z.string().nullable(),
  email: z.string().email(),
  name: z.string(),
  role: userRoleSchema,
  status: userStatusSchema,
});

export const authCompanyDtoSchema = z.object({
  id: z.string(),
  name: z.string(),
  slug: z.string(),
  email: nullableString,
  country: nullableString,
  status: companyStatusSchema,
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
  company: authCompanyDtoSchema.nullable(),
});

export const meResponseSchema = z.object({
  user: authUserDtoSchema,
  company: authCompanyDtoSchema.nullable(),
  branding: authBrandingDtoSchema,
  settings: authSettingsDtoSchema,
});

export const loginFormSchema = z.object({
  email: z.string().email("Indique um email válido."),
  password: z.string().min(1, "Indique a password."),
});

export const forgotPasswordFormSchema = z.object({
  email: z.string().email("Indique um email válido."),
});

export const forgotPasswordResponseSchema = z.object({
  message: z.string(),
  debugToken: z.string().optional(),
  debugResetLink: z.string().optional(),
});

export const resetPasswordFormSchema = z
  .object({
    password: z
      .string()
      .min(8, "Use pelo menos 8 caracteres.")
      .max(128, "Use no máximo 128 caracteres."),
    passwordConfirmation: z.string().min(1, "Confirme a senha."),
  })
  .refine((value) => value.password === value.passwordConfirmation, {
    path: ["passwordConfirmation"],
    message: "As senhas não coincidem.",
  });

export const resetPasswordResponseSchema = z.object({
  message: z.string(),
});

export type AuthUserDto = z.infer<typeof authUserDtoSchema>;
export type AuthCompanyDto = z.infer<typeof authCompanyDtoSchema>;
export type UserRole = z.infer<typeof userRoleSchema>;
export type UserStatus = z.infer<typeof userStatusSchema>;
export type CompanyStatus = z.infer<typeof companyStatusSchema>;
export type AuthResponse = z.infer<typeof authResponseSchema>;
export type MeResponse = z.infer<typeof meResponseSchema>;
export type LoginFormValues = z.infer<typeof loginFormSchema>;
export type ForgotPasswordRequest = z.infer<typeof forgotPasswordFormSchema>;
export type ForgotPasswordResponse = z.infer<
  typeof forgotPasswordResponseSchema
>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordFormSchema>;
export type ResetPasswordRequest = {
  token: string;
  newPassword: string;
};
export type ResetPasswordResponse = z.infer<typeof resetPasswordResponseSchema>;
