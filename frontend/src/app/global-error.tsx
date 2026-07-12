"use client";

export default function GlobalError() {
  return (
    <html lang="pt-PT">
      <body>
        <main className="min-h-dvh bg-background px-5 py-16 text-foreground">
          <div className="mx-auto max-w-3xl">
            <h1>Erro inesperado</h1>
            <p>Recarregue a página ou tente novamente mais tarde.</p>
          </div>
        </main>
      </body>
    </html>
  );
}
