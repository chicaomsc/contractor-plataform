"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import { readTokenFromHash } from "@/lib/auth/token-fragment";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { resetPassword } from "../api/auth-api";
import {
  resetPasswordFormSchema,
  type ResetPasswordFormValues,
} from "../types/auth";

const GENERIC_RESET_ERROR =
  "O link de recuperação é inválido ou não está mais disponível.";

function getResetErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 422) {
    const detail = error.body?.detail;
    return detail === "A nova senha deve ser diferente da atual."
      ? detail
      : GENERIC_RESET_ERROR;
  }

  if (error instanceof ApiError && error.status === 400) {
    return "A senha não atende à política definida.";
  }

  if (
    error instanceof ApiError &&
    (error.status === 401 || error.status === 403)
  ) {
    return "Não foi possível concluir a recuperação.";
  }

  return "Não foi possível atualizar a senha.";
}

export function ResetPasswordPage() {
  const searchParams = useSearchParams();
  const isAdminVariant = searchParams.get("variant") === "admin";
  const loginHref = isAdminVariant ? "/admin/login" : "/login";
  const forgotHref = isAdminVariant
    ? "/forgot-password?variant=admin"
    : "/forgot-password";
  const didReadToken = useRef(false);
  const [token, setToken] = useState<string | null>(null);
  const [hasReadHash, setHasReadHash] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordFormSchema),
    defaultValues: {
      password: "",
      passwordConfirmation: "",
    },
  });

  useEffect(() => {
    if (didReadToken.current) return;
    didReadToken.current = true;
    const nextToken = readTokenFromHash(window.location.hash);
    setToken(nextToken);
    setHasReadHash(true);
    window.history.replaceState(
      null,
      "",
      `${window.location.pathname}${window.location.search}`,
    );
  }, []);

  const canSubmit = useMemo(
    () => Boolean(token && !successMessage),
    [successMessage, token],
  );

  async function onSubmit(values: ResetPasswordFormValues) {
    if (!token) {
      setFormError(GENERIC_RESET_ERROR);
      return;
    }

    setFormError(null);
    try {
      const result = await resetPassword({
        token,
        newPassword: values.password,
      });
      setToken(null);
      setSuccessMessage(result.message);
    } catch (error) {
      setFormError(getResetErrorMessage(error));
    }
  }

  if (hasReadHash && !token && !successMessage) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12 text-center">
        <section className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8 rounded-lg">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
            Recuperação
          </p>
          <h1 className="m-0 mt-2 font-display text-3xl font-bold">
            Link inválido
          </h1>
          <p className="m-0 mt-4 border border-error bg-background px-4 py-3 text-sm font-semibold text-error rounded-lg">
            {GENERIC_RESET_ERROR}
          </p>
          <Link
            href={forgotHref}
            className="mt-6 inline-flex min-h-12 items-center justify-center border border-border px-5 py-3 text-sm font-semibold no-underline hover:border-primary rounded-lg"
          >
            Voltar para Esqueci minha senha
          </Link>
        </section>
      </main>
    );
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
            Definir nova senha
          </h1>
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Crie uma nova senha.
          </p>
        </div>

        <div className="mt-8 space-y-5">
          <label className="block space-y-2">
            <span className="text-sm font-semibold">Nova senha</span>
            <input
              type="password"
              autoComplete="new-password"
              disabled={!canSubmit || isSubmitting}
              className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary disabled:opacity-60 rounded-lg"
              {...register("password")}
            />
            {errors.password ? (
              <span className="block text-sm font-semibold text-error">
                {errors.password.message}
              </span>
            ) : null}
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-semibold">Confirmar senha</span>
            <input
              type="password"
              autoComplete="new-password"
              disabled={!canSubmit || isSubmitting}
              className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary disabled:opacity-60 rounded-lg"
              {...register("passwordConfirmation")}
            />
            {errors.passwordConfirmation ? (
              <span className="block text-sm font-semibold text-error">
                {errors.passwordConfirmation.message}
              </span>
            ) : null}
          </label>
        </div>

        {formError ? (
          <p className="mt-5 border border-error bg-background px-4 py-3 text-sm font-semibold text-erro rounded-lg">
            {formError}
          </p>
        ) : null}

        {successMessage ? (
          <p className="mt-5 border border-success bg-background px-4 py-3 text-sm font-semibold text-success rounded-lg">
            {successMessage}
          </p>
        ) : null}
        <div className="text-right">
          <Link
            href={loginHref}
            className="mt-4 inline-flex text-sm font-semibold no-underline hover:underline"
          >
            Voltar ao login
          </Link>
        </div>
        <Button
          className="mt-6 w-full rounded-lg"
          type="submit"
          disabled={!canSubmit || isSubmitting}
        >
          {isSubmitting ? "A atualizar" : "Atualizar senha"}
        </Button>
      </form>
    </main>
  );
}
