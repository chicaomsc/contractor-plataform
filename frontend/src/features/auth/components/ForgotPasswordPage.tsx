"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import { Copy, ExternalLink } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { forgotPassword } from "../api/auth-api";
import {
  forgotPasswordFormSchema,
  type ForgotPasswordResponse,
  type ForgotPasswordRequest,
} from "../types/auth";

const GENERIC_SUCCESS_MESSAGE =
  "Se existir uma conta para este e-mail, as instruções foram geradas.";

function getForgotErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 400) {
    return "Indique um e-mail válido.";
  }

  return "Não foi possível gerar as instruções. Tente novamente.";
}

function withAdminVariant(resetLink: string, enabled: boolean) {
  if (!enabled) return resetLink;

  try {
    const url = new URL(resetLink, window.location.origin);
    url.searchParams.set("variant", "admin");
    return url.toString();
  } catch {
    return resetLink;
  }
}

export function ForgotPasswordPage() {
  const searchParams = useSearchParams();
  const isAdminVariant = searchParams.get("variant") === "admin";
  const loginHref = isAdminVariant ? "/admin/login" : "/login";
  const [response, setResponse] = useState<ForgotPasswordResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const debugResetLink = response?.debugResetLink
    ? withAdminVariant(response.debugResetLink, isAdminVariant)
    : null;
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordRequest>({
    resolver: zodResolver(forgotPasswordFormSchema),
    defaultValues: { email: "" },
  });

  async function onSubmit(values: ForgotPasswordRequest) {
    setFormError(null);
    setResponse(null);
    setCopied(false);

    try {
      const result = await forgotPassword(values);
      setResponse(result);
    } catch (error) {
      setFormError(getForgotErrorMessage(error));
    }
  }

  async function copyDebugLink() {
    if (!debugResetLink) return;
    await navigator.clipboard.writeText(debugResetLink);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12">
      <form
        className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8 rounded-xl"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <div className="space-y-2 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
            Recuperação
          </p>
          <h1 className="m-0 font-display text-3xl font-bold">
            Recuperar Senha
          </h1>
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Informe o e-mail da conta.
          </p>
        </div>

        <div className="mt-8">
          <label className="block space-y-2">
            <span className="text-sm font-semibold">Email</span>
            <input
              type="email"
              autoComplete="email"
              className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary rounded-lg"
              {...register("email")}
            />
            {errors.email ? (
              <span className="block text-sm font-semibold text-error">
                {errors.email.message}
              </span>
            ) : null}
          </label>
        </div>

        {formError ? (
          <p className="mt-5 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
            {formError}
          </p>
        ) : null}

        {response ? (
          <p className="mt-5 border border-success bg-background px-4 py-3 text-sm font-semibold text-success rounded-lg">
            {GENERIC_SUCCESS_MESSAGE}
          </p>
        ) : null}

        {debugResetLink ? (
          <section className="mt-5 border border-warning bg-background p-4 rounded-lg">
            <p className="m-0 text-sm font-bold">Desenvolvimento local</p>
            <p className="m-0 mt-1 text-xs text-[var(--muted-foreground)]">
              Este link só aparece fora de produção quando um token novo foi
              criado.
            </p>
            <div className="mt-3 flex flex-col gap-3">
              <a
                href={debugResetLink}
                className="inline-flex min-h-11 items-center justify-center gap-2 border border-border px-4 py-2 text-sm font-semibold no-underline hover:border-primary rounded-lg"
              >
                <ExternalLink size={16} aria-hidden="true" />
                Abrir link de recuperação
              </a>
              <Button
                className="rounded-lg"
                type="button"
                variant="secondary"
                onClick={copyDebugLink}
              >
                <Copy size={16} aria-hidden="true" />
                {copied ? "Copiado" : "Copiar link"}
              </Button>
            </div>
          </section>
        ) : null}

        <div className="text-right">
          <Link
            href={loginHref}
            className="mt-5 inline-flex text-sm font-semibold no-underline hover:underline"
          >
            Voltar ao login
          </Link>
        </div>

        <Button
          className="mt-8 w-full rounded-lg"
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting ? "A enviar" : "Enviar instruções"}
        </Button>
      </form>
    </main>
  );
}
