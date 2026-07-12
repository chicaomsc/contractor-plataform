import Link from "next/link";

export default function NotFound() {
  return (
    <main className="min-h-dvh bg-background px-5 py-16 text-foreground">
      <div className="mx-auto max-w-3xl">
        <h1 className="font-display text-[var(--text-display-md)] font-semibold">
          Esta página não existe
        </h1>
        <p className="text-[var(--muted-foreground)]">
          O endereço pode ter mudado ou sido removido.
        </p>
        <Link className="font-semibold underline hover:text-primary" href="/">
          Voltar ao início
        </Link>
      </div>
    </main>
  );
}
