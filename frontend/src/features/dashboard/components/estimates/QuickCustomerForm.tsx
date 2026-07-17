"use client";

import { Save, X } from "lucide-react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  quickCustomerFormSchema,
  type QuickCustomerFormInput,
} from "../../types/customers";
import { zodResolver } from "../../utils/zod-resolver";
import { SaveFeedback } from "../DashboardState";
import { Field, inputClassName, textareaClassName } from "../FormControls";

const defaultValues: QuickCustomerFormInput = {
  name: "",
  email: "",
  phone: "",
  taxNumber: "",
  address: { street: "", city: "", postalCode: "", region: "", country: "" },
  notes: "",
};

type QuickCustomerFormProps = {
  isSaving: boolean;
  isError: boolean;
  onSubmit: (values: QuickCustomerFormInput) => Promise<void>;
  onCancel: () => void;
};

export function QuickCustomerForm({
  isSaving,
  isError,
  onSubmit,
  onCancel,
}: QuickCustomerFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<QuickCustomerFormInput>({
    resolver: zodResolver(quickCustomerFormSchema),
    defaultValues,
  });

  return (
    <form
      className="space-y-6 border border-border bg-surface p-6"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="m-0 font-display text-xl font-semibold">
            Criar cliente rapidamente
          </h2>
          <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
            Indique pelo menos um contacto (email ou telefone).
          </p>
        </div>
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
          <X size={16} aria-hidden="true" />
          Cancelar
        </Button>
      </div>

      <SaveFeedback isError={isError} isSuccess={false} />

      <Field label="Nome" error={errors.name}>
        <input className={inputClassName} {...register("name")} />
      </Field>

      <div className="grid gap-6 sm:grid-cols-2">
        <Field label="Email" error={errors.email}>
          <input type="email" className={inputClassName} {...register("email")} />
        </Field>
        <Field label="Telefone" error={errors.phone}>
          <input className={inputClassName} {...register("phone")} />
        </Field>
      </div>

      <Field label="NIF" error={errors.taxNumber}>
        <input className={inputClassName} {...register("taxNumber")} />
      </Field>

      <Field label="Notas" error={errors.notes}>
        <textarea className={textareaClassName} {...register("notes")} />
      </Field>

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button type="submit" disabled={isSaving}>
          <Save size={16} aria-hidden="true" />
          {isSaving ? "A criar" : "Criar cliente"}
        </Button>
      </div>
    </form>
  );
}
