import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";

export function TechnicalPlaceholder() {
  return (
    <Section labelledBy="foundation-title" className="min-h-[48vh]">
      <Container size="narrow">
        <p className="m-0 mb-3 text-xs font-semibold uppercase tracking-[0.12em] text-primary">
          Sprint 7B
        </p>
        <h1
          id="foundation-title"
          className="m-0 font-display text-[var(--text-display-md)] font-semibold leading-tight"
        >
          Fundação frontend ativa
        </h1>
        <p className="mt-4 text-[var(--muted-foreground)]">
          Layout base, tokens visuais, integração REST e estados globais estão
          preparados. As secções públicas da landing serão implementadas numa
          sprint posterior.
        </p>
      </Container>
    </Section>
  );
}
