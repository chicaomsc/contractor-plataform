import { formatMoney } from "../../utils/money";
import type { EstimateDto } from "../../types/estimates";
import { TotalsCard } from "./TotalsCard";

type EstimateSummaryProps = {
  estimate: EstimateDto;
};

/**
 * Every value rendered here comes straight from the EstimateResponse returned by the
 * backend (laborSubtotal, materialSubtotal, subtotal, vatAmount, total, upfrontAmount,
 * remainingAmount). This component performs no arithmetic — it only formats numbers the
 * API already calculated.
 */
export function EstimateSummary({ estimate }: EstimateSummaryProps) {
  const money = (value: number) => formatMoney(value, estimate.currency);

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <TotalsCard label="Mão de obra" value={money(estimate.laborSubtotal)} />
        <TotalsCard label="Materiais" value={money(estimate.materialSubtotal)} />
        <TotalsCard label="Subtotal" value={money(estimate.subtotal)} />
        <TotalsCard
          label={`IVA (${estimate.vatRate}%)`}
          value={money(estimate.vatAmount)}
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <TotalsCard label="Total" value={money(estimate.total)} emphasis />
        <TotalsCard
          label={`Entrada (${estimate.upfrontPercentage}%)`}
          value={money(estimate.upfrontAmount)}
        />
        <TotalsCard label="Saldo" value={money(estimate.remainingAmount)} />
      </div>
    </div>
  );
}
