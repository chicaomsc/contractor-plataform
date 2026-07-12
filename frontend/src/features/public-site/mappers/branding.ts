import type { PublicSiteViewModel } from "../types/view-model";

const HEX_COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/;

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

export function getBrandingStyle(
  site: PublicSiteViewModel | null,
): Record<string, string> {
  const style: Record<string, string> = {};
  const primary = site?.branding.primaryColor;
  const accent = site?.branding.accentColor;

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
