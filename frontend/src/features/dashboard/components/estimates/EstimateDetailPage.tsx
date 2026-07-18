"use client";

import { Pencil, Trash2, X } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { useCustomers } from "../../hooks/customer-hooks";
import { useDeleteEstimate, useEstimate, useUpdateEstimate } from "../../hooks/estimate-hooks";
import type {
  EstimateDto,
  EstimateGeneralInfoInput,
  EstimateItemFormInput,
  MaterialFormInput,
} from "../../types/estimates";
import { estimateUnitLabels } from "../../types/estimates";
import { formatMoney } from "../../utils/money";
import { ErrorState, LoadingState, SaveFeedback } from "../DashboardState";
import { PageHeader } from "../PageHeader";
import { StatusBadge } from "./StatusBadge";
import { DownloadPdfButton } from "./DownloadPdfButton";
import { EstimateGeneralInfoForm } from "./EstimateGeneralInfoForm";
import { EstimateItemsEditor } from "./EstimateItemsEditor";
import { EstimateNotEditableNotice } from "./EstimateNotEditableNotice";
import { EstimateStatusControl } from "./EstimateStatusControl";
import { EstimateSummary } from "./EstimateSummary";
import { MaterialsEditor } from "./MaterialsEditor";

const generalInfoFormId = "estimate-edit-general-info-form";

function toGeneralInfo(estimate: EstimateDto): EstimateGeneralInfoInput {
  return {
    title: estimate.title,
    description: estimate.description ?? "",
    validUntil: estimate.validUntil ?? "",
    notes: estimate.notes ?? "",
    terms: estimate.terms ?? "",
  };
}

function toItemInputs(estimate: EstimateDto): EstimateItemFormInput[] {
  return estimate.items.map((item) => ({
    serviceId: item.serviceId,
    description: item.description,
    quantity: item.quantity,
    unit: item.unit,
    unitPrice: item.unitPrice,
  }));
}

function toMaterialInputs(estimate: EstimateDto): MaterialFormInput[] {
  return estimate.materials.map((material) => ({
    name: material.name,
    description: material.description ?? "",
    quantity: material.quantity,
    unit: material.unit,
    unitPrice: material.unitPrice,
  }));
}

export function EstimateDetailPage({ estimateId }: { estimateId: string }) {
  const router = useRouter();
  const estimateQuery = useEstimate(estimateId);
  const customersQuery = useCustomers();
  const updateMutation = useUpdateEstimate();
  const deleteMutation = useDeleteEstimate();

  const [isEditing, setIsEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [generalInfo, setGeneralInfo] = useState<EstimateGeneralInfoInput | null>(null);
  const [items, setItems] = useState<EstimateItemFormInput[]>([]);
  const [materials, setMaterials] = useState<MaterialFormInput[]>([]);

  const estimate = estimateQuery.data;

  useEffect(() => {
    if (!estimate) return;
    setGeneralInfo(toGeneralInfo(estimate));
    setItems(toItemInputs(estimate));
    setMaterials(toMaterialInputs(estimate));
  }, [estimate]);

  if (estimateQuery.isLoading) {
    return <LoadingState label="A carregar orçamento" />;
  }

  if (estimateQuery.isError || !estimate) {
    return (
      <ErrorState
        title="Não foi possível carregar o orçamento"
        description="O orçamento pode não existir ou pertencer a outra empresa."
        onRetry={() => void estimateQuery.refetch()}
      />
    );
  }

  const currentEstimate: EstimateDto = estimate;
  const customerName =
    customersQuery.data?.find((customer) => customer.id === estimate.customerId)?.name ??
    "Cliente";
  const isDraft = estimate.status === "DRAFT";

  async function saveChanges(values: EstimateGeneralInfoInput) {
    await updateMutation.mutateAsync({
      estimateId,
      payload: {
        customerId: currentEstimate.customerId,
        ...values,
        items,
        materials,
      },
    });
    setGeneralInfo(values);
    setIsEditing(false);
  }

  function cancelEditing() {
    setGeneralInfo(toGeneralInfo(currentEstimate));
    setItems(toItemInputs(currentEstimate));
    setMaterials(toMaterialInputs(currentEstimate));
    setIsEditing(false);
  }

  async function confirmDelete() {
    await deleteMutation.mutateAsync(estimateId);
    router.push("/dashboard/estimates");
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow={estimate.number}
        title={estimate.title}
        description={`Cliente: ${customerName}`}
        action={
          <div className="flex flex-col items-end gap-3 sm:flex-row sm:items-center">
            <StatusBadge status={estimate.status} />
            <DownloadPdfButton estimateId={estimate.id} estimateNumber={estimate.number} />
          </div>
        }
      />

      <EstimateSummary estimate={estimate} />

      {!isDraft ? <EstimateNotEditableNotice status={estimate.status} /> : null}

      {isDraft && isEditing ? (
        <div className="space-y-6">
          <SaveFeedback isError={updateMutation.isError} isSuccess={false} />

          <div className="border border-border bg-surface p-6">
            <EstimateGeneralInfoForm
              formId={generalInfoFormId}
              defaultValues={generalInfo ?? toGeneralInfo(estimate)}
              onSubmit={(values) => void saveChanges(values)}
            />
          </div>

          <div className="border border-border bg-surface p-6">
            <EstimateItemsEditor items={items} onChange={setItems} />
          </div>

          <div className="border border-border bg-surface p-6">
            <MaterialsEditor materials={materials} onChange={setMaterials} />
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
            <Button type="button" variant="secondary" onClick={cancelEditing}>
              <X size={16} aria-hidden="true" />
              Cancelar
            </Button>
            <Button
              type="submit"
              form={generalInfoFormId}
              disabled={updateMutation.isPending}
            >
              {updateMutation.isPending ? "A guardar" : "Guardar alterações"}
            </Button>
          </div>
          <p className="m-0 text-right text-xs text-[var(--muted-foreground)]">
            Guardar submete o formulário de informações gerais e envia também os itens e
            materiais atuais.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          <section className="space-y-3 border border-border bg-surface p-6">
            <div className="flex items-center justify-between">
              <h3 className="m-0 font-display text-lg font-semibold">
                Itens ({estimate.items.length})
              </h3>
            </div>
            {estimate.items.length === 0 ? (
              <p className="m-0 text-sm text-[var(--muted-foreground)]">Nenhum item.</p>
            ) : (
              <ul className="m-0 list-none space-y-2 p-0 text-sm">
                {estimate.items.map((item) => (
                  <li key={item.id} className="flex justify-between gap-4">
                    <span>
                      {item.description} — {item.quantity} {estimateUnitLabels[item.unit]}
                    </span>
                    <span className="shrink-0 font-semibold">
                      {formatMoney(item.total, estimate.currency)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="space-y-3 border border-border bg-surface p-6">
            <h3 className="m-0 font-display text-lg font-semibold">
              Materiais ({estimate.materials.length})
            </h3>
            {estimate.materials.length === 0 ? (
              <p className="m-0 text-sm text-[var(--muted-foreground)]">
                Nenhum material.
              </p>
            ) : (
              <ul className="m-0 list-none space-y-2 p-0 text-sm">
                {estimate.materials.map((material) => (
                  <li key={material.id} className="flex justify-between gap-4">
                    <span>
                      {material.name} — {material.quantity}{" "}
                      {estimateUnitLabels[material.unit]}
                    </span>
                    <span className="shrink-0 font-semibold">
                      {formatMoney(material.total, estimate.currency)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {isDraft ? (
            <div className="flex justify-end">
              <Button type="button" onClick={() => setIsEditing(true)}>
                <Pencil size={16} aria-hidden="true" />
                Editar orçamento
              </Button>
            </div>
          ) : null}
        </div>
      )}

      <EstimateStatusControl estimateId={estimateId} currentStatus={estimate.status} />

      {isDraft ? (
        <div className="border border-error bg-surface p-4">
          <h3 className="m-0 font-display text-lg font-semibold">Excluir orçamento</h3>
          <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
            Só é possível excluir enquanto o orçamento está em Rascunho. Fora disso, use
            a transição de status para Cancelado.
          </p>
          {confirmingDelete ? (
            <div className="mt-4 flex flex-col gap-3 sm:flex-row">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setConfirmingDelete(false)}
                disabled={deleteMutation.isPending}
              >
                Cancelar
              </Button>
              <Button
                type="button"
                onClick={() => void confirmDelete()}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? "A excluir" : "Confirmar exclusão"}
              </Button>
            </div>
          ) : (
            <Button
              type="button"
              variant="secondary"
              className="mt-4"
              onClick={() => setConfirmingDelete(true)}
            >
              <Trash2 size={16} aria-hidden="true" />
              Excluir orçamento
            </Button>
          )}
        </div>
      ) : null}
    </div>
  );
}
