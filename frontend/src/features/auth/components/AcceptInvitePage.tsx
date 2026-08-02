"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import { readTokenFromHash } from "@/lib/auth/token-fragment";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { acceptInvite } from "../api/auth-api";
import { persistAuthSession } from "../api/auth-storage";
import { passwordFieldSchema } from "../types/auth";

const acceptInviteFormSchema = z
  .object({
    password: passwordFieldSchema,
    passwordConfirmation: z.string().min(1, "Confirme a password."),
  })
  .refine((value) => value.password === value.passwordConfirmation, {
    path: ["passwordConfirmation"],
    message: "As passwords não coincidem.",
  });

type AcceptInviteFormValues = z.infer<typeof acceptInviteFormSchema>;

function getInviteErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 404) {
    return "Este convite não existe ou já não está disponível.";
  }

  if (error instanceof ApiError && error.status === 422) {
    return "Este convite expirou, foi revogado ou já foi usado.";
  }

  if (error instanceof ApiError && error.status === 400) {
    return "Verifique a password indicada.";
  }

  return "Não foi possível aceitar o convite.";
}

export function AcceptInvitePage() {
  const didReadToken = useRef(false);
  const [token, setToken] = useState<string | null>(null);
  const [hasReadHash, setHasReadHash] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AcceptInviteFormValues>({
    resolver: zodResolver(acceptInviteFormSchema),
    defaultValues: {
      password: "",
      passwordConfirmation: "",
    },
  });

  // Same fragment-only handling as ResetPasswordPage (SEC-AUTH-07, Sprint 11B.6D):
  // the token is read once from window.location.hash — never the query string, so
  // it never reaches the server in a request line/Referer/access log — then the URL
  // is rewritten immediately so it also never lingers in browser history, and it's
  // kept only in this component's state, never localStorage/sessionStorage.
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

  async function onSubmit(values: AcceptInviteFormValues) {
    if (!token) {
      setFormError("O link de convite está incompleto.");
      return;
    }

    setFormError(null);
    try {
      const auth = await acceptInvite({ token, password: values.password });
      persistAuthSession(auth);
      window.location.assign("/dashboard");
    } catch (error) {
      setFormError(getInviteErrorMessage(error));
    }
  }

  const canSubmit = Boolean(token);

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12">
      <form
        className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <div className="space-y-2">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
            Convite
          </p>
          <h1 className="m-0 font-display text-3xl font-bold">
            Definir password
          </h1>
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Crie a password da conta OWNER para entrar no dashboard.
          </p>
        </div>

        <div className="mt-8 space-y-5">
          <label className="block space-y-2">
            <span className="text-sm font-semibold">Password</span>
            <input
              type="password"
              autoComplete="new-password"
              disabled={!canSubmit || isSubmitting}
              className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary disabled:opacity-60"
              {...register("password")}
            />
            {errors.password ? (
              <span className="block text-sm font-semibold text-error">
                {errors.password.message}
              </span>
            ) : null}
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-semibold">Confirmar password</span>
            <input
              type="password"
              autoComplete="new-password"
              disabled={!canSubmit || isSubmitting}
              className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary disabled:opacity-60"
              {...register("passwordConfirmation")}
            />
            {errors.passwordConfirmation ? (
              <span className="block text-sm font-semibold text-error">
                {errors.passwordConfirmation.message}
              </span>
            ) : null}
          </label>
        </div>

        {(hasReadHash && !token) || formError ? (
          <p className="mt-5 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
            {formError ?? "O link de convite está incompleto."}
          </p>
        ) : null}

        <Button
          className="mt-8 w-full"
          type="submit"
          disabled={isSubmitting || !canSubmit}
        >
          {isSubmitting ? "A ativar" : "Ativar conta"}
        </Button>
      </form>
    </main>
  );
}
