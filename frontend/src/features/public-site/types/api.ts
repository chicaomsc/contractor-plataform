import { z } from "zod";

const nullableString = z.string().nullable();

export const publicSiteBrandingDtoSchema = z
  .object({
    logoUrl: nullableString,
    primaryColor: nullableString,
    secondaryColor: nullableString,
    accentColor: nullableString,
    tagline: nullableString,
    aboutText: nullableString,
    footerText: nullableString,
  })
  .nullable();

export const publicSiteLocationDtoSchema = z
  .object({
    city: nullableString,
    region: nullableString,
    country: nullableString,
  })
  .nullable();

export const publicSiteDtoSchema = z.object({
  slug: z.string(),
  name: z.string(),
  tradeName: nullableString,
  publicPhone: nullableString,
  whatsapp: nullableString,
  website: nullableString,
  location: publicSiteLocationDtoSchema,
  branding: publicSiteBrandingDtoSchema,
});

export const publicServiceDtoSchema = z.object({
  id: z.string(),
  name: z.string(),
  slug: z.string(),
  shortDescription: nullableString,
  description: nullableString,
  icon: nullableString,
  displayOrder: z.number(),
});

export const publicGalleryItemDtoSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: nullableString,
  beforeImageUrl: nullableString,
  afterImageUrl: nullableString,
  displayOrder: z.number(),
  featured: z.boolean(),
});

export const publicServicesDtoSchema = z.array(publicServiceDtoSchema);
export const publicGalleryDtoSchema = z.array(publicGalleryItemDtoSchema);

export type PublicSiteDto = z.infer<typeof publicSiteDtoSchema>;
export type PublicServiceDto = z.infer<typeof publicServiceDtoSchema>;
export type PublicGalleryItemDto = z.infer<typeof publicGalleryItemDtoSchema>;
