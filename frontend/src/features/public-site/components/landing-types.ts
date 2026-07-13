import type {
  PublicGalleryItemViewModel,
  PublicServiceViewModel,
  PublicSiteViewModel,
} from "../types/view-model";

export type PublicLandingData = {
  site: PublicSiteViewModel;
  services: PublicServiceViewModel[];
  gallery: PublicGalleryItemViewModel[];
};

export type NavLink = {
  label: string;
  href: string;
};
