import { BRAND_PALETTES, type BrandPalette } from "./brand-palettes";

type BrandPaletteSelectorProps = {
  onApply: (palette: BrandPalette) => void;
};

export function BrandPaletteSelector({ onApply }: BrandPaletteSelectorProps) {
  return (
    <section aria-labelledby="brand-palettes-title" className="lg:col-span-3">
      <div>
        <h3 id="brand-palettes-title" className="m-0 text-sm font-semibold">
          Paletas sugeridas
        </h3>
        <p className="m-0 mt-1 text-xs text-[var(--muted-foreground)]">
          Aplique uma combinação inicial e ajuste os HEX antes de guardar.
        </p>
      </div>
      <div className="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
        {BRAND_PALETTES.map((palette) => (
          <button
            key={palette.id}
            type="button"
            className="border border-border bg-background px-3 py-2 text-left transition-colors hover:border-primary focus:border-primary focus:outline-none focus:ring-2 focus:ring-[var(--focus)]"
            onClick={() => onApply(palette)}
          >
            <span className="flex items-center justify-between gap-3">
              <span className="min-w-0 text-sm font-semibold">
                {palette.name}
              </span>
              <span className="shrink-0 text-xs font-semibold text-primary">
                Aplicar
              </span>
            </span>
            <span
              className="mt-2 grid grid-cols-3 overflow-hidden border border-border"
              aria-label={`Cores da paleta ${palette.name}`}
            >
              {[
                palette.primaryColor,
                palette.secondaryColor,
                palette.accentColor,
              ].map((color) => (
                <span
                  key={color}
                  className="h-5"
                  aria-label={color}
                  style={{ backgroundColor: color }}
                />
              ))}
            </span>
          </button>
        ))}
      </div>
    </section>
  );
}
