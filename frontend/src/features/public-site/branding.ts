import type { PublicSite } from "@/types/public-site";

const HEX_COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/;

export const FALLBACK_SITE: PublicSite = {
  slug: "contractor-platform",
  name: "Contractor Platform",
  tradeName: null,
  publicPhone: null,
  whatsapp: null,
  website: null,
  location: null,
  branding: {
    logoUrl: null,
    primaryColor: null,
    secondaryColor: null,
    accentColor: null,
    tagline: null,
    aboutText: null,
    footerText: "Informação pública temporariamente indisponível.",
  },
};

export function isSafeHexColor(
  value: string | null | undefined,
): value is string {
  return typeof value === "string" && HEX_COLOR_PATTERN.test(value);
}

function darkenHex(hex: string, amount: number) {
  const value = hex.replace("#", "");
  const channels = [0, 2, 4].map((index) =>
    Math.max(0, parseInt(value.slice(index, index + 2), 16) - amount),
  );
  return `#${channels.map((channel) => channel.toString(16).padStart(2, "0")).join("")}`;
}

export function getBrandingStyle(site: PublicSite): Record<string, string> {
  const primary = site.branding?.primaryColor;
  const accent = site.branding?.accentColor;
  const style: Record<string, string> = {};

  if (isSafeHexColor(primary)) {
    style["--primary"] = primary;
    style["--primary-hover"] = darkenHex(primary, 28);
    style["--focus"] = primary;
  }

  if (isSafeHexColor(accent)) {
    style["--accent"] = accent;
  }

  return style;
}

export function getDisplayName(site: PublicSite) {
  return site.tradeName ?? site.name;
}
