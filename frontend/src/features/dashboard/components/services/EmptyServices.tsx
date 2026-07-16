import { Wrench } from "lucide-react";
import { Button } from "@/components/ui/Button";

export function EmptyServices({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="border border-border bg-surface p-8 text-center">
      <div className="mx-auto flex size-12 items-center justify-center border border-border text-primary">
        <Wrench size={22} aria-hidden="true" />
      </div>
      <h2 className="m-0 mt-5 font-display text-2xl font-semibold">
        Sem serviços configurados
      </h2>
      <p className="mx-auto mt-2 max-w-xl text-sm text-[var(--muted-foreground)]">
        Crie os serviços oferecidos pela empresa. A landing pública passa a
        refletir a lista ativa retornada pela API pública.
      </p>
      <Button type="button" className="mt-6" onClick={onCreate}>
        Criar serviço
      </Button>
    </div>
  );
}
