"use client";

import { Save, X } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  serviceFormSchema,
  type ServiceDto,
  type ServiceFormInput,
} from "../../types/admin";
import { nullableText } from "../../utils/forms";
import { zodResolver } from "../../utils/zod-resolver";
import { SaveFeedback } from "../DashboardState";
import { Field, inputClassName, textareaClassName } from "../FormControls";

type ServiceFormProps = {
  service: ServiceDto | null;
  nextDisplayOrder: number;
  isSaving: boolean;
  isError: boolean;
  isSuccess: boolean;
  onSubmit: (values: ServiceFormInput) => Promise<void>;
  onCancel: () => void;
};

function toFormValues(
  service: ServiceDto | null,
  nextDisplayOrder: number,
): ServiceFormInput {
  return {
    name: service?.name ?? "",
    shortDescription: nullableText(service?.shortDescription),
    description: nullableText(service?.description),
    icon: nullableText(service?.icon),
    displayOrder: service?.displayOrder ?? nextDisplayOrder,
    active: service?.active ?? true,
  };
}

export function ServiceForm({
  service,
  nextDisplayOrder,
  isSaving,
  isError,
  isSuccess,
  onSubmit,
  onCancel,
}: ServiceFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ServiceFormInput>({
    resolver: zodResolver(serviceFormSchema),
    defaultValues: toFormValues(service, nextDisplayOrder),
  });

  useEffect(() => {
    reset(toFormValues(service, nextDisplayOrder));
  }, [nextDisplayOrder, reset, service]);

  return (
    <form
      className="space-y-6 border border-border bg-surface p-6"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="m-0 font-display text-2xl font-semibold">
            {service ? "Editar serviço" : "Criar serviço"}
          </h2>
          <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
            Dados usados na apresentação pública dos serviços.
          </p>
        </div>
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
          <X size={16} aria-hidden="true" />
          Fechar
        </Button>
      </div>

      <SaveFeedback isError={isError} isSuccess={isSuccess && !isDirty} />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_180px]">
        <Field label="Nome" error={errors.name}>
          <input className={inputClassName} {...register("name")} />
        </Field>
        <Field label="Ordem" error={errors.displayOrder}>
          <input
            type="number"
            min={0}
            className={inputClassName}
            {...register("displayOrder")}
          />
        </Field>
      </div>

      <Field
        label="Descrição curta"
        error={errors.shortDescription}
        hint="Texto breve usado como resumo quando disponível."
      >
        <input className={inputClassName} {...register("shortDescription")} />
      </Field>

      <Field label="Descrição" error={errors.description}>
        <textarea className={textareaClassName} {...register("description")} />
      </Field>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_220px]">
        <Field label="Ícone" error={errors.icon}>
          <input
            className={inputClassName}
            placeholder="Ex.: brush"
            {...register("icon")}
          />
        </Field>
        <label className="flex min-h-12 items-center gap-3 border border-border bg-background px-4 text-sm font-semibold">
          <input
            type="checkbox"
            className="size-4 accent-[var(--primary)]"
            {...register("active")}
          />
          Serviço ativo
        </label>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSaving || (!isDirty && Boolean(service))}>
          <Save size={16} aria-hidden="true" />
          {isSaving ? "A guardar" : "Guardar serviço"}
        </Button>
      </div>
    </form>
  );
}
