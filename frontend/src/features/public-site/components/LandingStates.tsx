import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";

export function LandingLoadingState() {
  return (
    <Section labelledBy="landing-loading-title" className="min-h-[60vh]">
      <Container>
        <div aria-busy="true" className="grid gap-10 lg:grid-cols-2">
          <div>
            <h1 id="landing-loading-title" className="sr-only">
              A carregar landing pública
            </h1>
            <div className="h-3 w-20 animate-pulse bg-primary" />
            <div className="mt-8 h-16 max-w-xl animate-pulse bg-surface-muted" />
            <div className="mt-5 h-24 max-w-2xl animate-pulse bg-surface-muted" />
            <div className="mt-8 h-12 w-48 animate-pulse bg-surface-muted" />
          </div>
          <div className="aspect-[4/3] animate-pulse bg-surface-muted lg:aspect-[5/6]" />
        </div>
      </Container>
    </Section>
  );
}

export function LandingBlockingError({
  title,
  message,
}: {
  title: string;
  message: string;
}) {
  return (
    <Section labelledBy="landing-error-title" className="min-h-[56vh]">
      <Container size="narrow">
        <div
          role="alert"
          tabIndex={-1}
          className="border-l-[3px] border-error bg-surface p-6"
        >
          <h1
            id="landing-error-title"
            className="m-0 font-display text-3xl font-bold"
          >
            {title}
          </h1>
          <p className="mb-0 mt-4 text-[var(--muted-foreground)]">{message}</p>
        </div>
      </Container>
    </Section>
  );
}
