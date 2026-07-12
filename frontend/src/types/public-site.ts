export type PublicSiteBranding = {
  logoUrl: string | null;
  primaryColor: string | null;
  secondaryColor: string | null;
  accentColor: string | null;
  tagline: string | null;
  aboutText: string | null;
  footerText: string | null;
};

export type PublicSiteLocation = {
  city: string | null;
  region: string | null;
  country: string | null;
};

export type PublicSite = {
  slug: string;
  name: string;
  tradeName: string | null;
  publicPhone: string | null;
  whatsapp: string | null;
  website: string | null;
  location: PublicSiteLocation | null;
  branding: PublicSiteBranding | null;
};

export type PublicService = {
  id: string;
  name: string;
  slug: string;
  shortDescription: string | null;
  description: string | null;
  icon: string | null;
  displayOrder: number;
};

export type PublicGalleryItem = {
  id: string;
  title: string;
  description: string | null;
  beforeImageUrl: string | null;
  afterImageUrl: string | null;
  displayOrder: number;
  featured: boolean;
};
