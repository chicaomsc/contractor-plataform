"use client";

import { useForm } from "react-hook-form";
import {
  estimateGeneralInfoSchema,
  type EstimateGeneralInfoInput,
} from "../../types/estimates";
import { zodResolver } from "../../utils/zod-resolver";
import { Field, inputClassName, textareaClassName } from "../FormControls";

type EstimateGeneralInfoFormProps = {
  formId: string;
  defaultValues: EstimateGeneralInfoInput;
  onSubmit: (values: EstimateGeneralInfoInput) => void;
};

export function EstimateGeneralInfoForm({
  formId,
  defaultValues,
  onSubmit,
}: EstimateGeneralInfoFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<EstimateGeneralInfoInput>({
    resolver: zodResolver(estimateGeneralInfoSchema),
    defaultValues,
  });

  return (
    <form
      id={formId}
      className="space-y-6"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <Field label="Título" error={errors.title}>
        <input className={inputClassName} {...register("title")} />
      </Field>

      <Field label="Descrição" error={errors.description}>
        <textarea className={textareaClassName} {...register("description")} />
      </Field>

      <Field
        label="Válido até"
        error={errors.validUntil}
        hint="Se não indicado, o backend aplica a validade padrão configurada em Settings."
      >
        <input type="date" className={inputClassName} {...register("validUntil")} />
      </Field>

      <Field label="Notas" error={errors.notes} hint="Visível para o cliente.">
        <textarea className={textareaClassName} {...register("notes")} />
      </Field>

      <Field label="Condições" error={errors.terms} hint="Condições comerciais do orçamento.">
        <textarea className={textareaClassName} {...register("terms")} />
      </Field>
    </form>
  );
}
