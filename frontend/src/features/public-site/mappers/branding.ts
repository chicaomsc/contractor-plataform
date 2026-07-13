import type { PublicSiteViewModel } from "../types/view-model";

const HEX_COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/;
const LIGHT_TEXT = "#ffffff";
const WARM_BACKGROUND = "#f8f6f2";
const MUTED_BACKGROUND = "#eceae5";
const MIN_CONTRAST = 4.5;

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

function getRelativeLuminance(hex: string) {
  const value = hex.replace("#", "");
  const [red, green, blue] = [0, 2, 4].map((index) => {
    const channel = parseInt(value.slice(index, index + 2), 16) / 255;
    return channel <= 0.03928
      ? channel / 12.92
      : Math.pow((channel + 0.055) / 1.055, 2.4);
  });

  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function getContrastRatio(foreground: string, background: string) {
  const foregroundLuminance = getRelativeLuminance(foreground);
  const backgroundLuminance = getRelativeLuminance(background);
  const lighter = Math.max(foregroundLuminance, backgroundLuminance);
  const darker = Math.min(foregroundLuminance, backgroundLuminance);

  return (lighter + 0.05) / (darker + 0.05);
}

function hasRequiredContrast(color: string) {
  return [LIGHT_TEXT, WARM_BACKGROUND, MUTED_BACKGROUND].every(
    (background) => getContrastRatio(color, background) >= MIN_CONTRAST,
  );
}

function getAccessiblePrimary(hex: string) {
  let color = hex;

  for (let step = 0; step < 16; step += 1) {
    if (hasRequiredContrast(color)) {
      return color;
    }

    color = darkenHex(color, 12);
  }

  return "#b43f08";
}

export function getBrandingStyle(
  site: PublicSiteViewModel | null,
): Record<string, string> {
  const style: Record<string, string> = {};
  const primary = site?.branding.primaryColor;
  const accent = site?.branding.accentColor;

  if (isSafeHexColor(primary)) {
    const accessiblePrimary = getAccessiblePrimary(primary);

    style["--primary"] = accessiblePrimary;
    style["--primary-hover"] = darkenHex(accessiblePrimary, 20);
    style["--primary-active"] = darkenHex(accessiblePrimary, 36);
    style["--focus"] = accessiblePrimary;
  }

  if (isSafeHexColor(accent)) {
    style["--accent"] = accent;
  }

  return style;
}
