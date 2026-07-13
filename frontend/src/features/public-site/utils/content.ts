import type {
  PublicGalleryItemViewModel,
  PublicSiteViewModel,
} from "../types/view-model";

export function getHeroHeadline(site: PublicSiteViewModel) {
  if (site.tagline) {
    return site.tagline;
  }

  if (site.locationLabel) {
    return `Pinturas e remodelações em ${site.locationLabel}`;
  }

  return "Pinturas e remodelações com trabalho organizado";
}

export function getHeroLead(site: PublicSiteViewModel) {
  const location = site.locationLabel ? ` em ${site.locationLabel}` : "";

  return `Serviço profissional${location}, com atenção ao acabamento, à organização da obra e ao contacto claro com o cliente.`;
}

export function getPrimaryGalleryImage(
  gallery: PublicGalleryItemViewModel[],
): PublicGalleryItemViewModel | null {
  return (
    gallery.find((item) => item.hasCompleteBeforeAfterPair && item.featured) ??
    gallery.find((item) => item.hasCompleteBeforeAfterPair) ??
    gallery.find((item) => item.hasAfterImage || item.hasBeforeImage) ??
    null
  );
}
