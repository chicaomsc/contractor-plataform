export const HEX_COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/;

const DARK_TEXT = "#111827";
const LIGHT_TEXT = "#FFFFFF";

export function isValidHexColor(
  value: string | null | undefined,
): value is string {
  return typeof value === "string" && HEX_COLOR_PATTERN.test(value);
}

export function normalizeHexColor(value: string) {
  return value.toUpperCase();
}

export function safeHexOrFallback(
  value: string | null | undefined,
  fallback: string,
) {
  return isValidHexColor(value) ? normalizeHexColor(value) : fallback;
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

export function getReadableTextColor(value: string | null | undefined) {
  if (!isValidHexColor(value)) {
    return LIGHT_TEXT;
  }

  return getRelativeLuminance(value) > 0.52 ? DARK_TEXT : LIGHT_TEXT;
}
