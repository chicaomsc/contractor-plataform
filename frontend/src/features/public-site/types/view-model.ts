export type BrandingTokenViewModel = {
  primaryColor: string | null;
  accentColor: string | null;
  logoUrl: string | null;
  hasValidPrimaryColor: boolean;
  hasValidAccentColor: boolean;
};

export type PublicSiteViewModel = {
  slug: string;
  name: string;
  displayName: string;
  publicPhone: string | null;
  whatsapp: string | null;
  website: string | null;
  locationLabel: string | null;
  branding: BrandingTokenViewModel;
  tagline: string | null;
  aboutText: string | null;
  footerText: string | null;
};

export type PublicServiceViewModel = {
  id: string;
  name: string;
  slug: string;
  summary: string | null;
  displayOrder: number;
};

export type PublicGalleryItemViewModel = {
  id: string;
  title: string;
  description: string | null;
  beforeImageUrl: string | null;
  afterImageUrl: string | null;
  beforeAlt: string | null;
  afterAlt: string | null;
  displayOrder: number;
  featured: boolean;
  hasBeforeImage: boolean;
  hasAfterImage: boolean;
  hasCompleteBeforeAfterPair: boolean;
};

export type PublicSiteIntegrationViewModel = {
  site: PublicSiteViewModel;
  services: PublicServiceViewModel[];
  gallery: PublicGalleryItemViewModel[];
};
