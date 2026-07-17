"use client";

import { ArrowLeft, ArrowRight, Check } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils/cn";
import { useCreateCustomer } from "../../hooks/customer-hooks";
import { useCreateEstimate } from "../../hooks/estimate-hooks";
import type { CustomerDto, QuickCustomerFormInput } from "../../types/customers";
import type {
  EstimateGeneralInfoInput,
  EstimateItemFormInput,
  MaterialFormInput,
} from "../../types/estimates";
import { SaveFeedback } from "../DashboardState";
import { CustomerSelector } from "./CustomerSelector";
import { EstimateGeneralInfoForm } from "./EstimateGeneralInfoForm";
import { EstimateItemsEditor } from "./EstimateItemsEditor";
import { EstimateReviewStep } from "./EstimateReviewStep";
import { MaterialsEditor } from "./MaterialsEditor";
import { QuickCustomerForm } from "./QuickCustomerForm";

const steps = [
  { key: "customer", label: "Cliente" },
  { key: "general", label: "Informações gerais" },
  { key: "items", label: "Itens" },
  { key: "materials", label: "Materiais" },
  { key: "review", label: "Revisão" },
] as const;

type StepKey = (typeof steps)[number]["key"];

const generalInfoFormId = "estimate-general-info-form";

export function EstimateWizard() {
  const router = useRouter();
  const createEstimateMutation = useCreateEstimate();
  const createCustomerMutation = useCreateCustomer();

  const [stepIndex, setStepIndex] = useState(0);
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerDto | null>(null);
  const [showQuickCreate, setShowQuickCreate] = useState(false);
  const [generalInfo, setGeneralInfo] = useState<EstimateGeneralInfoInput | null>(null);
  const [items, setItems] = useState<EstimateItemFormInput[]>([]);
  const [materials, setMaterials] = useState<MaterialFormInput[]>([]);

  const currentStep: StepKey = steps[stepIndex].key;

  async function handleQuickCreateCustomer(values: QuickCustomerFormInput) {
    const customer = await createCustomerMutation.mutateAsync(values);
    setSelectedCustomer(customer);
    setShowQuickCreate(false);
  }

  function goNext() {
    setStepIndex((index) => Math.min(index + 1, steps.length - 1));
  }

  function goBack() {
    setStepIndex((index) => Math.max(index - 1, 0));
  }

  async function handleCreate() {
    if (!selectedCustomer || !generalInfo) {
      return;
    }

    const estimate = await createEstimateMutation.mutateAsync({
      customerId: selectedCustomer.id,
      ...generalInfo,
      items,
      materials,
    });

    router.push(`/dashboard/estimates/${estimate.id}`);
  }

  return (
    <div className="space-y-8">
      <ol className="m-0 flex list-none flex-wrap gap-x-2 gap-y-3 p-0">
        {steps.map((step, index) => {
          const isActive = index === stepIndex;
          const isDone = index < stepIndex;

          return (
            <li key={step.key} className="flex items-center gap-2">
              <span
                className={cn(
                  "flex size-8 items-center justify-center border text-xs font-bold",
                  isActive && "border-primary bg-primary text-primary-foreground",
                  isDone && !isActive && "border-success text-success",
                  !isActive && !isDone && "border-border text-[var(--muted-foreground)]",
                )}
              >
                {isDone ? <Check size={14} aria-hidden="true" /> : index + 1}
              </span>
              <span
                className={cn(
                  "text-sm font-semibold",
                  isActive ? "text-foreground" : "text-[var(--muted-foreground)]",
                )}
              >
                {step.label}
              </span>
              {index < steps.length - 1 ? (
                <span className="mx-1 hidden h-px w-6 bg-border sm:block" aria-hidden="true" />
              ) : null}
            </li>
          );
        })}
      </ol>

      <div className="border border-border bg-surface p-6">
        {currentStep === "customer" ? (
          showQuickCreate ? (
            <QuickCustomerForm
              isSaving={createCustomerMutation.isPending}
              isError={createCustomerMutation.isError}
              onSubmit={handleQuickCreateCustomer}
              onCancel={() => setShowQuickCreate(false)}
            />
          ) : (
            <CustomerSelector
              selectedCustomerId={selectedCustomer?.id ?? null}
              onSelect={setSelectedCustomer}
              onCreateNew={() => setShowQuickCreate(true)}
            />
          )
        ) : null}

        {currentStep === "general" ? (
          <EstimateGeneralInfoForm
            formId={generalInfoFormId}
            defaultValues={
              generalInfo ?? {
                title: "",
                description: "",
                validUntil: "",
                notes: "",
                terms: "",
              }
            }
            onSubmit={(values) => {
              setGeneralInfo(values);
              goNext();
            }}
          />
        ) : null}

        {currentStep === "items" ? (
          <EstimateItemsEditor items={items} onChange={setItems} />
        ) : null}

        {currentStep === "materials" ? (
          <MaterialsEditor materials={materials} onChange={setMaterials} />
        ) : null}

        {currentStep === "review" && selectedCustomer && generalInfo ? (
          <div className="space-y-6">
            <EstimateReviewStep
              customer={selectedCustomer}
              generalInfo={generalInfo}
              items={items}
              materials={materials}
            />
            <SaveFeedback isError={createEstimateMutation.isError} isSuccess={false} />
          </div>
        ) : null}
      </div>

      <div className="flex items-center justify-between">
        <Button
          type="button"
          variant="secondary"
          onClick={goBack}
          disabled={stepIndex === 0}
        >
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar
        </Button>

        {currentStep === "review" ? (
          <Button
            type="button"
            onClick={() => void handleCreate()}
            disabled={createEstimateMutation.isPending}
          >
            {createEstimateMutation.isPending ? "A criar" : "Criar orçamento"}
          </Button>
        ) : currentStep === "general" ? (
          <Button type="submit" form={generalInfoFormId}>
            Avançar
            <ArrowRight size={16} aria-hidden="true" />
          </Button>
        ) : (
          <Button
            type="button"
            onClick={goNext}
            disabled={currentStep === "customer" && !selectedCustomer}
          >
            Avançar
            <ArrowRight size={16} aria-hidden="true" />
          </Button>
        )}
      </div>
    </div>
  );
}
