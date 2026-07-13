import type {
  PublicGalleryItemDto,
  PublicServiceDto,
  PublicSiteDto,
} from "../types/api";
import type {
  PublicGalleryItemViewModel,
  PublicServiceViewModel,
  PublicSiteViewModel,
} from "../types/view-model";
import { isSafeHexColor } from "./branding";

function compactLocation(parts: Array<string | null>) {
  const label = parts.filter(Boolean).join(", ");
  return label.length > 0 ? label : null;
}

export function mapPublicSiteDto(dto: PublicSiteDto): PublicSiteViewModel {
  return {
    slug: dto.slug,
    name: dto.name,
    displayName: dto.tradeName ?? dto.name,
    publicPhone: dto.publicPhone,
    whatsapp: dto.whatsapp,
    website: dto.website,
    locationLabel: dto.location
      ? compactLocation([
          dto.location.city,
          dto.location.region,
          dto.location.country,
        ])
      : null,
    branding: {
      logoUrl: dto.branding?.logoUrl ?? null,
      primaryColor: isSafeHexColor(dto.branding?.primaryColor)
        ? dto.branding.primaryColor
        : null,
      accentColor: isSafeHexColor(dto.branding?.accentColor)
        ? dto.branding.accentColor
        : null,
      hasValidPrimaryColor: isSafeHexColor(dto.branding?.primaryColor),
      hasValidAccentColor: isSafeHexColor(dto.branding?.accentColor),
    },
    tagline: dto.branding?.tagline ?? null,
    aboutText: dto.branding?.aboutText ?? null,
    footerText: dto.branding?.footerText ?? null,
  };
}

export function mapPublicServiceDto(
  dto: PublicServiceDto,
): PublicServiceViewModel {
  return {
    id: dto.id,
    name: dto.name,
    slug: dto.slug,
    summary: dto.shortDescription ?? dto.description,
    displayOrder: dto.displayOrder,
  };
}

export function mapPublicGalleryItemDto(
  dto: PublicGalleryItemDto,
): PublicGalleryItemViewModel {
  const hasBeforeImage = Boolean(dto.beforeImageUrl);
  const hasAfterImage = Boolean(dto.afterImageUrl);

  return {
    id: dto.id,
    title: dto.title,
    description: dto.description,
    beforeImageUrl: dto.beforeImageUrl,
    afterImageUrl: dto.afterImageUrl,
    beforeAlt: dto.beforeImageUrl ? `${dto.title} antes da intervenção` : null,
    afterAlt: dto.afterImageUrl ? `${dto.title} depois da intervenção` : null,
    displayOrder: dto.displayOrder,
    featured: dto.featured,
    hasBeforeImage,
    hasAfterImage,
    hasCompleteBeforeAfterPair: hasBeforeImage && hasAfterImage,
  };
}
