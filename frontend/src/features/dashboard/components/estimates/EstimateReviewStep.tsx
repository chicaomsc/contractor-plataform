import { Info } from "lucide-react";
import type { CustomerDto } from "../../types/customers";
import {
  estimateUnitLabels,
  type EstimateGeneralInfoInput,
  type EstimateItemFormInput,
  type MaterialFormInput,
} from "../../types/estimates";

type EstimateReviewStepProps = {
  customer: CustomerDto;
  generalInfo: EstimateGeneralInfoInput;
  items: EstimateItemFormInput[];
  materials: MaterialFormInput[];
};

/**
 * Deliberately shows no subtotal/VAT/total here — those don't exist until the backend
 * creates the estimate and calculates them. Showing a frontend-computed preview would
 * mean duplicating financial logic that belongs exclusively to the backend.
 */
export function EstimateReviewStep({
  customer,
  generalInfo,
  items,
  materials,
}: EstimateReviewStepProps) {
  return (
    <div className="space-y-6">
      <div className="flex gap-3 border border-border bg-background p-4 text-sm text-[var(--muted-foreground)]">
        <Info size={18} className="mt-0.5 shrink-0 text-primary" aria-hidden="true" />
        <p className="m-0">
          Número, subtotais, IVA, total e entrada são calculados pelo backend assim que o
          orçamento for criado — ficam visíveis na página do orçamento a seguir.
        </p>
      </div>

      <section className="space-y-2 border border-border bg-surface p-4">
        <h3 className="m-0 font-display text-lg font-semibold">Cliente</h3>
        <p className="m-0 font-semibold">{customer.name}</p>
        <p className="m-0 text-sm text-[var(--muted-foreground)]">
          {[customer.email, customer.phone].filter(Boolean).join(" · ") ||
            "Sem contacto registado"}
        </p>
      </section>

      <section className="space-y-2 border border-border bg-surface p-4">
        <h3 className="m-0 font-display text-lg font-semibold">Informações gerais</h3>
        <p className="m-0 font-semibold">{generalInfo.title}</p>
        {generalInfo.description ? (
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            {generalInfo.description}
          </p>
        ) : null}
        {generalInfo.validUntil ? (
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Válido até {generalInfo.validUntil}
          </p>
        ) : null}
      </section>

      <section className="space-y-2 border border-border bg-surface p-4">
        <h3 className="m-0 font-display text-lg font-semibold">
          Itens ({items.length})
        </h3>
        {items.length === 0 ? (
          <p className="m-0 text-sm text-[var(--muted-foreground)]">Nenhum item.</p>
        ) : (
          <ul className="m-0 list-none space-y-1 p-0 text-sm">
            {items.map((item, index) => (
              <li key={index} className="flex justify-between gap-4">
                <span>{item.description}</span>
                <span className="shrink-0 text-[var(--muted-foreground)]">
                  {item.quantity} {estimateUnitLabels[item.unit]}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="space-y-2 border border-border bg-surface p-4">
        <h3 className="m-0 font-display text-lg font-semibold">
          Materiais ({materials.length})
        </h3>
        {materials.length === 0 ? (
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Nenhum material.
          </p>
        ) : (
          <ul className="m-0 list-none space-y-1 p-0 text-sm">
            {materials.map((material, index) => (
              <li key={index} className="flex justify-between gap-4">
                <span>{material.name}</span>
                <span className="shrink-0 text-[var(--muted-foreground)]">
                  {material.quantity} {estimateUnitLabels[material.unit]}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
