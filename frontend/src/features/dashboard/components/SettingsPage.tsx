"use client";

import { Save } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  useSettings,
  useUpdateSettings,
} from "../hooks/dashboard-hooks";
import {
  updateSettingsSchema,
  type SettingsDto,
  type UpdateSettingsInput,
} from "../types/admin";
import { nullableText } from "../utils/forms";
import { zodResolver } from "../utils/zod-resolver";
import { ErrorState, LoadingState, SaveFeedback } from "./DashboardState";
import { Field, inputClassName, textareaClassName } from "./FormControls";
import { PageHeader } from "./PageHeader";

function toFormValues(settings: SettingsDto): UpdateSettingsInput {
  return {
    defaultCurrency: nullableText(settings.defaultCurrency),
    defaultTaxRate: settings.defaultTaxRate,
    estimateValidityDays: settings.estimateValidityDays,
    estimateFooterText: nullableText(settings.estimateFooterText),
    locale: nullableText(settings.locale),
    timezone: nullableText(settings.timezone),
    dateFormat: nullableText(settings.dateFormat),
    numberFormat: nullableText(settings.numberFormat),
  };
}

export function SettingsPage() {
  const settingsQuery = useSettings();
  const updateMutation = useUpdateSettings();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<UpdateSettingsInput>({
    resolver: zodResolver(updateSettingsSchema),
  });

  useEffect(() => {
    if (settingsQuery.data) {
      reset(toFormValues(settingsQuery.data));
    }
  }, [settingsQuery.data, reset]);

  async function onSubmit(values: UpdateSettingsInput) {
    const settings = await updateMutation.mutateAsync(values);
    reset(toFormValues(settings));
  }

  if (settingsQuery.isLoading) {
    return <LoadingState label="A carregar settings" />;
  }

  if (settingsQuery.isError || !settingsQuery.data) {
    return (
      <ErrorState
        title="Não foi possível carregar as settings"
        description="As configurações vêm de /settings/me."
        onRetry={() => void settingsQuery.refetch()}
      />
    );
  }

  return (
    <form className="space-y-8" onSubmit={handleSubmit(onSubmit)} noValidate>
      <PageHeader
        eyebrow="Settings"
        title="Editar configurações"
        description="Preferências administrativas existentes no backend para moeda, impostos, localização e formatos."
        action={
          <Button
            type="submit"
            disabled={!isDirty || updateMutation.isPending}
          >
            <Save size={16} aria-hidden="true" />
            {updateMutation.isPending ? "A guardar" : "Guardar"}
          </Button>
        }
      />

      <SaveFeedback
        isError={updateMutation.isError}
        isSuccess={updateMutation.isSuccess && !isDirty}
      />

      <section className="grid gap-6 border border-border bg-surface p-6 lg:grid-cols-3">
        <Field
          label="Moeda padrão"
          error={errors.defaultCurrency}
          hint="Código ISO de 3 letras."
        >
          <input
            className={inputClassName}
            maxLength={3}
            {...register("defaultCurrency")}
          />
        </Field>
        <Field label="Taxa padrão (%)" error={errors.defaultTaxRate}>
          <input
            type="number"
            step="0.01"
            className={inputClassName}
            {...register("defaultTaxRate")}
          />
        </Field>
        <Field
          label="Validade de orçamento (dias)"
          error={errors.estimateValidityDays}
        >
          <input
            type="number"
            className={inputClassName}
            {...register("estimateValidityDays")}
          />
        </Field>
        <Field label="Locale" error={errors.locale}>
          <input className={inputClassName} {...register("locale")} />
        </Field>
        <Field label="Timezone" error={errors.timezone}>
          <input className={inputClassName} {...register("timezone")} />
        </Field>
        <Field label="Formato de data" error={errors.dateFormat}>
          <input className={inputClassName} {...register("dateFormat")} />
        </Field>
        <Field label="Formato numérico" error={errors.numberFormat}>
          <input className={inputClassName} {...register("numberFormat")} />
        </Field>
        <div className="lg:col-span-3">
          <Field
            label="Texto padrão de rodapé do orçamento"
            error={errors.estimateFooterText}
          >
            <textarea
              className={textareaClassName}
              {...register("estimateFooterText")}
            />
          </Field>
        </div>
      </section>
    </form>
  );
}
