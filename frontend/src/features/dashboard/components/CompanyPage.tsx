"use client";

import { Save } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  useCompany,
  useUpdateCompany,
} from "../hooks/dashboard-hooks";
import {
  updateCompanySchema,
  type CompanyDto,
  type UpdateCompanyInput,
} from "../types/admin";
import { nullableText } from "../utils/forms";
import { zodResolver } from "../utils/zod-resolver";
import { ErrorState, LoadingState, SaveFeedback } from "./DashboardState";
import { Field, inputClassName } from "./FormControls";
import { PageHeader } from "./PageHeader";

function toFormValues(company: CompanyDto): UpdateCompanyInput {
  return {
    name: company.name,
    tradeName: nullableText(company.tradeName),
    email: nullableText(company.email),
    phone: nullableText(company.phone),
    whatsapp: nullableText(company.whatsapp),
    website: nullableText(company.website),
    taxNumber: nullableText(company.taxNumber),
    country: nullableText(company.country),
    address: {
      street: nullableText(company.address?.street),
      city: nullableText(company.address?.city),
      postalCode: nullableText(company.address?.postalCode),
      region: nullableText(company.address?.region),
      country: nullableText(company.address?.country),
    },
  };
}

export function CompanyPage() {
  const companyQuery = useCompany();
  const updateMutation = useUpdateCompany();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<UpdateCompanyInput>({
    resolver: zodResolver(updateCompanySchema),
  });

  useEffect(() => {
    if (companyQuery.data) {
      reset(toFormValues(companyQuery.data));
    }
  }, [companyQuery.data, reset]);

  async function onSubmit(values: UpdateCompanyInput) {
    const company = await updateMutation.mutateAsync(values);
    reset(toFormValues(company));
  }

  if (companyQuery.isLoading) {
    return <LoadingState label="A carregar empresa" />;
  }

  if (companyQuery.isError || !companyQuery.data) {
    return (
      <ErrorState
        title="Não foi possível carregar a empresa"
        description="Os dados da empresa vêm de /company/me."
        onRetry={() => void companyQuery.refetch()}
      />
    );
  }

  return (
    <form className="space-y-8" onSubmit={handleSubmit(onSubmit)} noValidate>
      <PageHeader
        eyebrow="Empresa"
        title="Editar dados da empresa"
        description="Dados administrativos usados pela plataforma e pela landing pública quando disponíveis."
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

      <section className="grid gap-6 border border-border bg-surface p-6 lg:grid-cols-2">
        <Field label="Nome legal" error={errors.name}>
          <input className={inputClassName} {...register("name")} />
        </Field>
        <Field label="Nome comercial" error={errors.tradeName}>
          <input className={inputClassName} {...register("tradeName")} />
        </Field>
        <Field label="Email" error={errors.email}>
          <input
            type="email"
            className={inputClassName}
            {...register("email")}
          />
        </Field>
        <Field label="Telefone" error={errors.phone}>
          <input className={inputClassName} {...register("phone")} />
        </Field>
        <Field label="WhatsApp" error={errors.whatsapp}>
          <input className={inputClassName} {...register("whatsapp")} />
        </Field>
        <Field label="Website" error={errors.website}>
          <input className={inputClassName} {...register("website")} />
        </Field>
        <Field label="NIF / Tax number" error={errors.taxNumber}>
          <input className={inputClassName} {...register("taxNumber")} />
        </Field>
        <Field label="País da empresa" error={errors.country}>
          <input
            className={inputClassName}
            maxLength={2}
            {...register("country")}
          />
        </Field>
      </section>

      <section className="space-y-5 border border-border bg-surface p-6">
        <h2 className="m-0 font-display text-2xl font-semibold">Morada</h2>
        <div className="grid gap-6 lg:grid-cols-2">
          <Field label="Rua" error={errors.address?.street}>
            <input className={inputClassName} {...register("address.street")} />
          </Field>
          <Field label="Cidade" error={errors.address?.city}>
            <input className={inputClassName} {...register("address.city")} />
          </Field>
          <Field label="Código postal" error={errors.address?.postalCode}>
            <input
              className={inputClassName}
              {...register("address.postalCode")}
            />
          </Field>
          <Field label="Região" error={errors.address?.region}>
            <input className={inputClassName} {...register("address.region")} />
          </Field>
          <Field label="País da morada" error={errors.address?.country}>
            <input
              className={inputClassName}
              maxLength={2}
              {...register("address.country")}
            />
          </Field>
        </div>
      </section>
    </form>
  );
}
