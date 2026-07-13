import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import { BeforeAfterComparisonBaseline } from "@/features/before-after-comparison";
import type { PublicGalleryItemViewModel } from "../types/view-model";
import { SectionLabel } from "./SectionLabel";

type GallerySectionProps = {
  gallery: PublicGalleryItemViewModel[];
  isPartialError?: boolean;
};

export function GallerySection({
  gallery,
  isPartialError = false,
}: GallerySectionProps) {
  const completePairs = gallery.filter(
    (item) => item.hasCompleteBeforeAfterPair,
  );
  const partialItems = gallery.filter(
    (item) => !item.hasCompleteBeforeAfterPair,
  );
  const featured = completePairs[0] ?? null;
  const remaining = completePairs.slice(1, 4);

  if (!isPartialError && gallery.length === 0) {
    return null;
  }

  return (
    <Section id="trabalhos" labelledBy="gallery-title" variant="muted">
      <Container className="space-y-12">
        <header className="grid gap-6 lg:grid-cols-[0.72fr_1fr] lg:items-end">
          <div className="max-w-2xl space-y-4">
            <SectionLabel>Trabalhos</SectionLabel>
            <h2
              id="gallery-title"
              className="m-0 font-display text-3xl font-bold md:text-4xl"
            >
              Antes e depois
            </h2>
          </div>
          <p className="m-0 max-w-2xl text-base leading-7 text-[var(--muted-foreground)]">
            Fotografias públicas carregadas pela empresa. Quando o par está
            completo, a comparação fica disponível no desktop e empilhada em
            ecrãs menores.
          </p>
        </header>

        {isPartialError ? (
          <p role="status" className="border-l-[3px] border-warning pl-4">
            Não foi possível carregar a galeria pública neste momento.
          </p>
        ) : null}

        {featured ? (
          <BeforeAfterComparisonBaseline
            title={featured.title}
            description={featured.description}
            beforeImageUrl={featured.beforeImageUrl}
            afterImageUrl={featured.afterImageUrl}
            beforeAlt={featured.beforeAlt}
            afterAlt={featured.afterAlt}
            titleHeadingLevel="h3"
          />
        ) : null}

        {remaining.length > 0 ? (
          <ul className="grid gap-8 lg:grid-cols-3">
            {remaining.map((item) => (
              <li key={item.id} className="border-t border-border pt-6">
                <BeforeAfterComparisonBaseline
                  title={item.title}
                  description={item.description}
                  beforeImageUrl={item.beforeImageUrl}
                  afterImageUrl={item.afterImageUrl}
                  beforeAlt={item.beforeAlt}
                  afterAlt={item.afterAlt}
                  titleHeadingLevel="h3"
                />
              </li>
            ))}
          </ul>
        ) : null}

        {!featured && partialItems.length > 0 ? (
          <div className="border-l-[3px] border-primary bg-background p-6">
            <p className="m-0 font-semibold">
              A galeria ainda não tem pares antes/depois completos.
            </p>
            <ul className="mt-4 grid gap-2 text-sm text-[var(--muted-foreground)]">
              {partialItems.slice(0, 4).map((item) => (
                <li key={item.id}>{item.title}</li>
              ))}
            </ul>
          </div>
        ) : null}
      </Container>
    </Section>
  );
}
