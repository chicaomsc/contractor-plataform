import { Container } from "@/components/layout/Container";

export default function Loading() {
  return (
    <main className="min-h-dvh bg-background py-16" aria-busy="true">
      <Container>
        <div className="h-4 w-40 animate-pulse bg-surface-muted" />
        <div className="mt-6 h-10 max-w-xl animate-pulse bg-surface-muted" />
        <div className="mt-4 h-24 max-w-2xl animate-pulse bg-surface-muted" />
      </Container>
    </main>
  );
}
