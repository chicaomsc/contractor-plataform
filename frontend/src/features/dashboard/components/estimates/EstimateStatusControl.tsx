"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { useChangeEstimateStatus } from "../../hooks/estimate-hooks";
import {
  estimateStatusLabels,
  estimateStatusValues,
  type EstimateStatus,
} from "../../types/estimates";
import { inputClassName } from "../FormControls";

type EstimateStatusControlProps = {
  estimateId: string;
  currentStatus: EstimateStatus;
};

/**
 * Every status here is offered as an option — the backend is the sole authority on which
 * transitions are actually valid (EstimateStatusTransitionService, 409 on violation). The
 * frontend does not re-implement that state machine; an invalid choice simply surfaces the
 * backend's rejection.
 */
export function EstimateStatusControl({
  estimateId,
  currentStatus,
}: EstimateStatusControlProps) {
  const changeStatusMutation = useChangeEstimateStatus();
  const [nextStatus, setNextStatus] = useState<EstimateStatus | "">("");

  const otherStatuses = estimateStatusValues.filter(
    (status) => status !== currentStatus,
  );

  async function submit() {
    if (!nextStatus) {
      return;
    }
    await changeStatusMutation.mutateAsync({ estimateId, status: nextStatus });
    setNextStatus("");
  }

  return (
    <div className="space-y-3 border border-border bg-surface p-4">
      <h3 className="m-0 font-display text-lg font-semibold">Alterar status</h3>
      <div className="flex flex-col gap-3 sm:flex-row">
        <select
          className={inputClassName}
          value={nextStatus}
          onChange={(event) => setNextStatus(event.target.value as EstimateStatus | "")}
          aria-label="Novo status"
        >
          <option value="">Selecione o novo status</option>
          {otherStatuses.map((status) => (
            <option key={status} value={status}>
              {estimateStatusLabels[status]}
            </option>
          ))}
        </select>
        <Button
          type="button"
          onClick={() => void submit()}
          disabled={!nextStatus || changeStatusMutation.isPending}
        >
          {changeStatusMutation.isPending ? "A atualizar" : "Confirmar"}
        </Button>
      </div>
      {changeStatusMutation.isError ? (
        <p className="m-0 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
          Não foi possível aplicar esta transição de status. O backend só permite
          transições específicas a partir de {estimateStatusLabels[currentStatus]}.
        </p>
      ) : null}
    </div>
  );
}
