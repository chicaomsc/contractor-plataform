export type BrandPalette = {
  id: string;
  name: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
};

export const BRAND_PALETTES: BrandPalette[] = [
  {
    id: "professional",
    name: "Profissional",
    primaryColor: "#1E40AF",
    secondaryColor: "#3B82F6",
    accentColor: "#F59E0B",
  },
  {
    id: "modern",
    name: "Moderna",
    primaryColor: "#0F766E",
    secondaryColor: "#14B8A6",
    accentColor: "#F97316",
  },
  {
    id: "elegant",
    name: "Elegante",
    primaryColor: "#312E81",
    secondaryColor: "#6366F1",
    accentColor: "#D97706",
  },
  {
    id: "natural",
    name: "Natural",
    primaryColor: "#166534",
    secondaryColor: "#65A30D",
    accentColor: "#CA8A04",
  },
  {
    id: "vibrant",
    name: "Vibrante",
    primaryColor: "#BE123C",
    secondaryColor: "#DB2777",
    accentColor: "#2563EB",
  },
  {
    id: "neutral",
    name: "Neutra",
    primaryColor: "#374151",
    secondaryColor: "#6B7280",
    accentColor: "#B45309",
  },
];
