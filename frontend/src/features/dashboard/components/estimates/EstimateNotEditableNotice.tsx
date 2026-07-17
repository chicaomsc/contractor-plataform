import { Lock } from "lucide-react";
import { estimateStatusLabels, type EstimateStatus } from "../../types/estimates";

export function EstimateNotEditableNotice({ status }: { status: EstimateStatus }) {
  return (
    <div className="flex gap-3 border border-warning bg-surface p-4">
      <Lock size={20} className="mt-0.5 shrink-0 text-warning" aria-hidden="true" />
      <div>
        <p className="m-0 font-semibold">Este orçamento não pode ser editado</p>
        <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
          O status atual é <strong>{estimateStatusLabels[status]}</strong>. Apenas
          orçamentos em Rascunho podem ter título, itens ou materiais alterados. Utilize
          as ações de status abaixo para avançar o orçamento.
        </p>
      </div>
    </div>
  );
}
