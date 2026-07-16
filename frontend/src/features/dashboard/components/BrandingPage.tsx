"use client";

import { Save } from "lucide-react";
import Image from "next/image";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  useBranding,
  useCompany,
  useUpdateBranding,
} from "../hooks/dashboard-hooks";
import {
  updateBrandingSchema,
  type BrandingDto,
  type UpdateBrandingInput,
} from "../types/admin";
import { resolveAdminAssetUrl } from "../utils/assets";
import { nullableText } from "../utils/forms";
import { zodResolver } from "../utils/zod-resolver";
import { ErrorState, LoadingState, SaveFeedback } from "./DashboardState";
import { Field, inputClassName, textareaClassName } from "./FormControls";
import { PageHeader } from "./PageHeader";

function toFormValues(branding: BrandingDto): UpdateBrandingInput {
  return {
    primaryColor: nullableText(branding.primaryColor),
    secondaryColor: nullableText(branding.secondaryColor),
    accentColor: nullableText(branding.accentColor),
    tagline: nullableText(branding.tagline),
    aboutText: nullableText(branding.aboutText),
    footerText: nullableText(branding.footerText),
    quotationPrefix: nullableText(branding.quotationPrefix),
    signatureName: nullableText(branding.signatureName),
  };
}

export function BrandingPage() {
  const brandingQuery = useBranding();
  const companyQuery = useCompany();
  const updateMutation = useUpdateBranding();
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isDirty },
  } = useForm<UpdateBrandingInput>({
    resolver: zodResolver(updateBrandingSchema),
  });

  useEffect(() => {
    if (brandingQuery.data) {
      reset(toFormValues(brandingQuery.data));
    }
  }, [brandingQuery.data, reset]);

  async function onSubmit(values: UpdateBrandingInput) {
    const branding = await updateMutation.mutateAsync(values);
    reset(toFormValues(branding));
  }

  if (brandingQuery.isLoading || companyQuery.isLoading) {
    return <LoadingState label="A carregar branding" />;
  }

  if (brandingQuery.isError || !brandingQuery.data) {
    return (
      <ErrorState
        title="Não foi possível carregar o branding"
        description="Os dados de branding vêm de /branding/me."
        onRetry={() => void brandingQuery.refetch()}
      />
    );
  }

  const values = watch();
  const logoUrl = resolveAdminAssetUrl(brandingQuery.data.logoUrl);
  const primary = values.primaryColor || "#1c1c1a";
  const accent = values.accentColor || "#b43f08";

  return (
    <form className="space-y-8" onSubmit={handleSubmit(onSubmit)} noValidate>
      <PageHeader
        eyebrow="Branding"
        title="Editar identidade visual"
        description="Cores e textos institucionais consumidos pela landing multi-tenant."
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

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <section className="grid gap-6 border border-border bg-surface p-6 lg:grid-cols-3">
          <Field label="Cor primária" error={errors.primaryColor}>
            <input
              type="text"
              className={inputClassName}
              placeholder="#1E40AF"
              {...register("primaryColor")}
            />
          </Field>
          <Field label="Cor secundária" error={errors.secondaryColor}>
            <input
              type="text"
              className={inputClassName}
              placeholder="#3B82F6"
              {...register("secondaryColor")}
            />
          </Field>
          <Field label="Cor de acento" error={errors.accentColor}>
            <input
              type="text"
              className={inputClassName}
              placeholder="#F59E0B"
              {...register("accentColor")}
            />
          </Field>
          <div className="lg:col-span-3">
            <Field label="Tagline" error={errors.tagline}>
              <input className={inputClassName} {...register("tagline")} />
            </Field>
          </div>
          <div className="lg:col-span-3">
            <Field label="Sobre" error={errors.aboutText}>
              <textarea
                className={textareaClassName}
                {...register("aboutText")}
              />
            </Field>
          </div>
          <div className="lg:col-span-3">
            <Field label="Texto de rodapé" error={errors.footerText}>
              <textarea
                className={textareaClassName}
                {...register("footerText")}
              />
            </Field>
          </div>
          <Field label="Prefixo de orçamento" error={errors.quotationPrefix}>
            <input
              className={inputClassName}
              {...register("quotationPrefix")}
            />
          </Field>
          <div className="lg:col-span-2">
            <Field label="Assinatura" error={errors.signatureName}>
              <input
                className={inputClassName}
                {...register("signatureName")}
              />
            </Field>
          </div>
        </section>

        <aside className="border border-border bg-surface p-6">
          <h2 className="m-0 font-display text-2xl font-semibold">
            Preview
          </h2>
          <div className="mt-6 overflow-hidden border border-border">
            <div className="flex min-h-16 items-center justify-between gap-4 bg-background px-5">
              {logoUrl ? (
                <Image
                  src={logoUrl}
                  alt="Logo atual"
                  width={72}
                  height={40}
                  className="h-10 w-auto object-contain"
                />
              ) : (
                <span className="font-display text-lg font-bold">
                  {companyQuery.data?.name}
                </span>
              )}
              <span
                className="px-4 py-2 text-sm font-semibold text-white"
                style={{ backgroundColor: primary }}
              >
                WhatsApp
              </span>
            </div>
            <div className="space-y-4 bg-background p-6">
              <div
                className="h-1 w-20"
                style={{ backgroundColor: primary }}
                aria-hidden="true"
              />
              <h3 className="m-0 font-display text-3xl font-bold leading-tight">
                {values.tagline || companyQuery.data?.name}
              </h3>
              <p className="m-0 text-sm text-[var(--muted-foreground)]">
                {values.aboutText || "Texto institucional ainda não definido."}
              </p>
              <div
                className="inline-flex min-h-11 items-center px-5 text-sm font-semibold text-white"
                style={{ backgroundColor: accent }}
              >
                Pedir orçamento
              </div>
            </div>
          </div>
        </aside>
      </div>
    </form>
  );
}
