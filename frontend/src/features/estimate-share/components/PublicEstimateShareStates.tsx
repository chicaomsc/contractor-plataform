import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";

export function ShareLoadingState() {
  return (
    <Section labelledBy="share-loading-title" className="min-h-[60vh]">
      <Container size="narrow">
        <div aria-busy="true">
          <h1 id="share-loading-title" className="sr-only">
            A carregar orçamento partilhado
          </h1>
          <div className="h-6 w-40 animate-pulse bg-surface-muted" />
          <div className="mt-6 h-32 animate-pulse bg-surface-muted" />
          <div className="mt-4 h-48 animate-pulse bg-surface-muted" />
        </div>
      </Container>
    </Section>
  );
}

export function ShareUnavailableState() {
  return (
    <Section labelledBy="share-error-title" className="min-h-[56vh]">
      <Container size="narrow">
        <div role="alert" className="border-l-[3px] border-error bg-surface p-6">
          <h1 id="share-error-title" className="m-0 font-display text-2xl font-bold">
            Link indisponível
          </h1>
          <p className="mb-0 mt-4 text-[var(--muted-foreground)]">
            Este link de orçamento não existe, expirou ou foi revogado pelo prestador.
            Contacte-o diretamente para obter um novo link.
          </p>
        </div>
      </Container>
    </Section>
  );
}
