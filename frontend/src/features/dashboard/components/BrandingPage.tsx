"use client";

import { ImageUp, Save, Trash2 } from "lucide-react";
import Image from "next/image";
import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import {
  useBranding,
  useCompany,
  useDeleteCompanyLogo,
  useUpdateBranding,
  useUploadCompanyLogo,
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

const ACCEPTED_LOGO_TYPES = new Set(["image/png", "image/jpeg", "image/webp"]);
const MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024;

function getLogoErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "Sessão expirada. Inicie sessão novamente.";
    }

    if (error.status === 403) {
      return "Apenas OWNER pode alterar a logo da empresa.";
    }

    if (error.status === 400 || error.status === 422) {
      return error.body?.detail ?? "O arquivo não cumpre a política de upload.";
    }

    return error.message;
  }

  return fallback;
}

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
  const uploadLogoMutation = useUploadCompanyLogo();
  const deleteLogoMutation = useDeleteCompanyLogo();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [logoError, setLogoError] = useState<string | null>(null);
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

  async function handleLogoSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    setLogoError(null);

    if (!file) {
      return;
    }

    if (!ACCEPTED_LOGO_TYPES.has(file.type)) {
      setLogoError("Use uma imagem PNG, JPG ou WebP.");
      return;
    }

    if (file.size > MAX_LOGO_SIZE_BYTES) {
      setLogoError("A logo deve ter no máximo 5 MB.");
      return;
    }

    try {
      await uploadLogoMutation.mutateAsync(file);
    } catch (error) {
      setLogoError(getLogoErrorMessage(error, "Não foi possível enviar a logo."));
    }
  }

  async function handleRemoveLogo() {
    setLogoError(null);

    if (!window.confirm("Remover a logo atual da empresa?")) {
      return;
    }

    try {
      await deleteLogoMutation.mutateAsync();
    } catch (error) {
      setLogoError(getLogoErrorMessage(error, "Não foi possível remover a logo."));
    }
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
  const companyName = companyQuery.data?.name ?? "Empresa";
  const isLogoMutationPending =
    uploadLogoMutation.isPending || deleteLogoMutation.isPending;
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
        <div className="space-y-6">
          <section className="border border-border bg-surface p-6">
            <div>
              <h2 className="m-0 font-display text-2xl font-semibold">
                Logo da empresa
              </h2>
              <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
                PNG, JPG ou WebP até 5 MB. A landing usa o nome da empresa quando não há logo.
              </p>
            </div>

            <div className="mt-5 flex flex-col gap-5 md:flex-row md:items-center">
              <div className="flex min-h-28 w-full items-center justify-center border border-border bg-background p-4 md:w-56">
                {logoUrl ? (
                  <Image
                    src={logoUrl}
                    alt={`Logo atual de ${companyName}`}
                    width={180}
                    height={96}
                    className="max-h-20 w-auto max-w-full object-contain"
                  />
                ) : (
                  <span className="text-center font-display text-lg font-bold">
                    {companyName}
                  </span>
                )}
              </div>

              <div className="min-w-0 flex-1">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  className="sr-only"
                  aria-label="Selecionar logo da empresa"
                  onChange={handleLogoSelected}
                  disabled={isLogoMutationPending}
                />
                <div className="flex flex-col gap-3 sm:flex-row">
                  <Button
                    type="button"
                    variant="primary"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isLogoMutationPending}
                  >
                    <ImageUp size={16} aria-hidden="true" />
                    {uploadLogoMutation.isPending
                      ? "A enviar"
                      : logoUrl
                        ? "Alterar logo"
                        : "Enviar logo"}
                  </Button>
                  {logoUrl ? (
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={handleRemoveLogo}
                      disabled={isLogoMutationPending}
                    >
                      <Trash2 size={16} aria-hidden="true" />
                      {deleteLogoMutation.isPending ? "A remover" : "Remover logo"}
                    </Button>
                  ) : null}
                </div>
                {logoError ? (
                  <p className="m-0 mt-3 text-sm font-semibold text-error">
                    {logoError}
                  </p>
                ) : null}
                {uploadLogoMutation.isSuccess || deleteLogoMutation.isSuccess ? (
                  <p className="m-0 mt-3 text-sm font-semibold text-success">
                    Logo atualizada.
                  </p>
                ) : null}
              </div>
            </div>
          </section>

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
        </div>

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
                  {companyName}
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
