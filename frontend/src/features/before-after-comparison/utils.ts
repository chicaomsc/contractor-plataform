export const COMPARISON_STEP = 5;

export function clampComparisonPosition(value: number): number {
  if (Number.isNaN(value)) {
    return 50;
  }

  return Math.min(100, Math.max(0, Math.round(value)));
}

export function getComparisonValueText(value: number): string {
  return `A mostrar ${value}% da imagem depois`;
}

export function hasCompleteComparisonImages(input: {
  beforeImageUrl?: string | null;
  afterImageUrl?: string | null;
  beforeAlt?: string | null;
  afterAlt?: string | null;
}): input is {
  beforeImageUrl: string;
  afterImageUrl: string;
  beforeAlt: string;
  afterAlt: string;
} {
  return Boolean(
    input.beforeImageUrl &&
    input.afterImageUrl &&
    input.beforeAlt &&
    input.afterAlt,
  );
}
