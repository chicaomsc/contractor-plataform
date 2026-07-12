"use client";

import { RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default function Error({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="min-h-dvh bg-background px-5 py-16 text-foreground">
      <div className="mx-auto max-w-3xl">
        <h1 className="font-display text-[var(--text-display-md)] font-semibold">
          Não foi possível carregar esta página
        </h1>
        <p className="text-[var(--muted-foreground)]">
          Tente novamente dentro de alguns instantes.
        </p>
        <Button type="button" onClick={reset}>
          <RotateCcw size={18} aria-hidden="true" />
          Tentar novamente
        </Button>
      </div>
    </main>
  );
}
