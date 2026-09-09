import Image from "next/image";
import { getReadableTextColor, safeHexOrFallback } from "../../utils/colors";
import type { UpdateBrandingInput } from "../../types/admin";

type BrandPreviewProps = {
  values: UpdateBrandingInput;
  logoUrl: string | null;
  companyName: string;
};

export function BrandPreview({
  values,
  logoUrl,
  companyName,
}: BrandPreviewProps) {
  const primary = safeHexOrFallback(values.primaryColor, "#1C1C1A");
  const secondary = safeHexOrFallback(values.secondaryColor, "#3B82F6");
  const accent = safeHexOrFallback(values.accentColor, "#B43F08");

  return (
    <aside className="border border-border bg-surface p-6">
      <h2 className="m-0 font-display text-2xl font-semibold">Preview</h2>
      <div className="mt-6 overflow-hidden border border-border">
        <div className="flex min-h-16 items-center justify-between gap-4 bg-background px-5">
          {logoUrl ? (
            <Image
              src={logoUrl}
              alt="Logo atual"
              width={72}
              height={40}
              className="h-10 w-auto object-contain"
            />
          ) : (
            <span className="font-display text-lg font-bold">
              {companyName}
            </span>
          )}
          <span
            className="px-4 py-2 text-sm font-semibold"
            style={{
              backgroundColor: primary,
              color: getReadableTextColor(primary),
            }}
          >
            WhatsApp
          </span>
        </div>
        <div className="space-y-4 bg-background p-6">
          <div
            className="h-1 w-20"
            style={{ backgroundColor: primary }}
            aria-hidden="true"
          />
          <h3 className="m-0 font-display text-3xl font-bold leading-tight">
            {values.tagline || companyName}
          </h3>
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            {values.aboutText || "Texto institucional ainda não definido."}
          </p>
          <div
            className="inline-flex min-h-11 items-center px-5 text-sm font-semibold"
            style={{
              backgroundColor: accent,
              color: getReadableTextColor(accent),
            }}
          >
            Pedir orçamento
          </div>
          <div
            className="border border-border p-3"
            aria-label="Amostra demonstrativa da paleta"
          >
            <span className="text-xs font-semibold uppercase text-[var(--muted-foreground)]">
              Paleta
            </span>
            <div className="mt-2 grid grid-cols-3 overflow-hidden border border-border">
              {[
                { color: primary, label: "principal" },
                { color: secondary, label: "auxiliar" },
                { color: accent, label: "destaque" },
              ].map(({ color, label }) => (
                <span
                  key={label}
                  className="h-8"
                  style={{ backgroundColor: color }}
                  aria-label={`Amostra ${label} ${color}`}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </aside>
  );
}
