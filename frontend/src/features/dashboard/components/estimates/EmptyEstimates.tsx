import { FileText } from "lucide-react";
import { Button } from "@/components/ui/Button";

export function EmptyEstimates({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="border border-border bg-surface p-8 text-center">
      <div className="mx-auto flex size-12 items-center justify-center border border-border text-primary">
        <FileText size={22} aria-hidden="true" />
      </div>
      <h2 className="m-0 mt-5 font-display text-2xl font-semibold">
        Sem orçamentos criados
      </h2>
      <p className="mx-auto mt-2 max-w-xl text-sm text-[var(--muted-foreground)]">
        Crie o primeiro orçamento para um cliente. Números, totais e IVA são
        calculados automaticamente pelo backend.
      </p>
      <Button type="button" className="mt-6" onClick={onCreate}>
        Criar orçamento
      </Button>
    </div>
  );
}
