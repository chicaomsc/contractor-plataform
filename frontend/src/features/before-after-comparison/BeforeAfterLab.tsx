import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import { BeforeAfterComparisonBaseline } from "./BeforeAfterComparisonBaseline";
import { BeforeAfterComparisonRefinedCandidate } from "./BeforeAfterComparisonRefinedCandidate";
import {
  beforeAfterComparisonFixture,
  incompleteBeforeAfterComparisonFixture,
} from "./fixtures";

export function BeforeAfterLab() {
  return (
    <main id="main-content">
      <Section labelledBy="lab-title">
        <Container className="space-y-12">
          <header className="max-w-3xl space-y-4">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-primary">
              Laboratório técnico
            </p>
            <h1 id="lab-title" className="font-display text-4xl font-bold">
              BeforeAfterComparison
            </h1>
            <p className="text-[var(--muted-foreground)]">
              Rota isolada para comparar a baseline manual e a versão refinada
              do spike. Este ecrã não pertence à landing pública.
            </p>
          </header>

          <div className="grid gap-12 xl:grid-cols-2">
            <section aria-labelledby="baseline-title" className="space-y-5">
              <h2
                id="baseline-title"
                className="border-b border-border pb-3 font-display text-2xl font-semibold"
              >
                A. Baseline manual
              </h2>
              <BeforeAfterComparisonBaseline
                {...beforeAfterComparisonFixture}
              />
            </section>

            <section aria-labelledby="impeccable-title" className="space-y-5">
              <h2
                id="impeccable-title"
                className="border-b border-border pb-3 font-display text-2xl font-semibold"
              >
                B. Versão refinada
              </h2>
              <BeforeAfterComparisonRefinedCandidate
                {...beforeAfterComparisonFixture}
              />
            </section>
          </div>

          <section aria-labelledby="fallback-title" className="space-y-6">
            <h2
              id="fallback-title"
              className="border-b border-border pb-3 font-display text-2xl font-semibold"
            >
              Estado com par incompleto
            </h2>
            <div className="grid gap-8 lg:grid-cols-2">
              <BeforeAfterComparisonBaseline
                {...incompleteBeforeAfterComparisonFixture}
              />
              <BeforeAfterComparisonRefinedCandidate
                {...incompleteBeforeAfterComparisonFixture}
              />
            </div>
          </section>
        </Container>
      </Section>
    </main>
  );
}
