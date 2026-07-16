import { z } from "zod";

const nullableString = z.string().nullable();
const nullableNumber = z.number().nullable();
const hexColor = z
  .string()
  .regex(/^#[0-9A-Fa-f]{6}$/, "Use uma cor HEX válida, ex. #1E40AF.");
const emptyToNull = (value: unknown) => (value === "" ? null : value);
const nullableTextInput = (max: number) =>
  z.preprocess(emptyToNull, z.string().max(max).nullable());
const nullableNumberInput = z.preprocess(
  emptyToNull,
  z.coerce.number().min(0).max(100).nullable(),
);

export const addressDtoSchema = z.object({
  street: nullableString,
  city: nullableString,
  postalCode: nullableString,
  region: nullableString,
  country: nullableString,
});

export const companyDtoSchema = z.object({
  id: z.string(),
  name: z.string(),
  tradeName: nullableString,
  slug: z.string(),
  email: nullableString,
  phone: nullableString,
  whatsapp: nullableString,
  website: nullableString,
  taxNumber: nullableString,
  country: nullableString,
  address: addressDtoSchema.nullable(),
  status: z.string(),
});

export const brandingDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  logoUrl: nullableString,
  primaryColor: nullableString,
  secondaryColor: nullableString,
  accentColor: nullableString,
  tagline: nullableString,
  aboutText: nullableString,
  footerText: nullableString,
  quotationPrefix: nullableString,
  signatureName: nullableString,
});

export const settingsDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  defaultCurrency: nullableString,
  defaultTaxRate: nullableNumber,
  estimateValidityDays: nullableNumber,
  estimateFooterText: nullableString,
  locale: nullableString,
  timezone: nullableString,
  dateFormat: nullableString,
  numberFormat: nullableString,
});

export const serviceDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  name: z.string(),
  slug: z.string(),
  shortDescription: nullableString,
  description: nullableString,
  icon: nullableString,
  displayOrder: z.number(),
  active: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const galleryDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  title: z.string(),
  description: nullableString,
  beforeImageUrl: nullableString,
  afterImageUrl: nullableString,
  displayOrder: z.number(),
  featured: z.boolean(),
  active: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const updateCompanySchema = z.object({
  name: z.string().min(2, "Indique pelo menos 2 caracteres.").max(255),
  tradeName: nullableTextInput(255),
  email: z.preprocess(
    emptyToNull,
    z.string().email("Indique um email válido.").max(255).nullable(),
  ),
  phone: nullableTextInput(50),
  whatsapp: nullableTextInput(50),
  website: nullableTextInput(500),
  taxNumber: nullableTextInput(50),
  country: nullableTextInput(2),
  address: z.object({
    street: nullableTextInput(255),
    city: nullableTextInput(100),
    postalCode: nullableTextInput(20),
    region: nullableTextInput(100),
    country: nullableTextInput(2),
  }),
});

export const updateBrandingSchema = z.object({
  primaryColor: z.preprocess(emptyToNull, hexColor.nullable()),
  secondaryColor: z.preprocess(emptyToNull, hexColor.nullable()),
  accentColor: z.preprocess(emptyToNull, hexColor.nullable()),
  tagline: nullableTextInput(500),
  aboutText: nullableTextInput(2000),
  footerText: nullableTextInput(2000),
  quotationPrefix: nullableTextInput(20),
  signatureName: nullableTextInput(255),
});

export const updateSettingsSchema = z.object({
  defaultCurrency: z.preprocess(
    emptyToNull,
    z.string().min(3).max(3).nullable(),
  ),
  defaultTaxRate: nullableNumberInput,
  estimateValidityDays: z.preprocess(
    emptyToNull,
    z.coerce.number().min(1).nullable(),
  ),
  estimateFooterText: nullableTextInput(2000),
  locale: nullableTextInput(10),
  timezone: nullableTextInput(50),
  dateFormat: nullableTextInput(50),
  numberFormat: nullableTextInput(50),
});

export const servicesDtoSchema = z.array(serviceDtoSchema);
export const galleryItemsDtoSchema = z.array(galleryDtoSchema);

export type CompanyDto = z.infer<typeof companyDtoSchema>;
export type BrandingDto = z.infer<typeof brandingDtoSchema>;
export type SettingsDto = z.infer<typeof settingsDtoSchema>;
export type ServiceDto = z.infer<typeof serviceDtoSchema>;
export type GalleryDto = z.infer<typeof galleryDtoSchema>;
export type UpdateCompanyInput = z.infer<typeof updateCompanySchema>;
export type UpdateBrandingInput = z.infer<typeof updateBrandingSchema>;
export type UpdateSettingsInput = z.infer<typeof updateSettingsSchema>;
