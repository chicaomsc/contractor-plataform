"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { ApiError } from "@/lib/api/errors";
import { readTokenFromHash } from "@/lib/auth/token-fragment";
import { acceptTeamInvitation } from "../api/auth-api";
import { persistAuthSession } from "../api/auth-storage";
import {
  acceptTeamInvitationFormSchema,
  type AcceptTeamInvitationFormValues,
} from "../types/auth";

const GENERIC_TEAM_INVITE_ERROR =
  "Este convite é inválido ou não está mais disponível.";

function getTeamInviteErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 422) {
    return GENERIC_TEAM_INVITE_ERROR;
  }

  if (error instanceof ApiError && error.status === 400) {
    return "Verifique o nome e a senha indicados.";
  }

  return "Não foi possível criar o acesso. Tente novamente.";
}

export function TeamInviteAcceptPage() {
  const didReadToken = useRef(false);
  const [token, setToken] = useState<string | null>(null);
  const [hasReadHash, setHasReadHash] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [hasAccepted, setHasAccepted] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AcceptTeamInvitationFormValues>({
    resolver: zodResolver(acceptTeamInvitationFormSchema),
    defaultValues: {
      name: "",
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
      window.location.pathname,
    );
  }, []);

  async function onSubmit(values: AcceptTeamInvitationFormValues) {
    if (!token) {
      setFormError(GENERIC_TEAM_INVITE_ERROR);
      return;
    }

    setFormError(null);
    try {
      const auth = await acceptTeamInvitation({
        token,
        name: values.name,
        password: values.password,
      });
      setHasAccepted(true);
      setToken(null);
      persistAuthSession(auth);
      window.location.assign("/dashboard");
    } catch (error) {
      setFormError(getTeamInviteErrorMessage(error));
    }
  }

  if (hasReadHash && !token && !hasAccepted) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12 text-center">
        <section className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
            Convite
          </p>
          <h1 className="m-0 mt-2 font-display text-3xl font-bold">
            Convite inválido
          </h1>
          <p className="m-0 mt-4 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
            {GENERIC_TEAM_INVITE_ERROR}
          </p>
          <Link
            href="/login"
            className="mt-6 inline-flex min-h-12 items-center justify-center border border-border px-5 py-3 text-sm font-semibold no-underline hover:border-primary"
          >
            Ir para o login
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12">
      <form
        className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <div className="space-y-2 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
            Convite
          </p>
          <h1 className="m-0 font-display text-3xl font-bold">
            Crie seu acesso
          </h1>
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Defina seu nome e senha para acessar a empresa.
          </p>
        </div>

        <div className="mt-8 space-y-5">
          <label className="block space-y-2">
            <span className="text-sm font-semibold">Nome</span>
            <input
              autoComplete="name"
              disabled={!token || isSubmitting || hasAccepted}
              className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary disabled:opacity-60"
              {...register("name")}
            />
            {errors.name ? (
              <span className="block text-sm font-semibold text-error">
                {errors.name.message}
              </span>
            ) : null}
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-semibold">Senha</span>
            <input
              type="password"
              autoComplete="new-password"
              disabled={!token || isSubmitting || hasAccepted}
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
            <span className="text-sm font-semibold">Confirmar senha</span>
            <input
              type="password"
              autoComplete="new-password"
              disabled={!token || isSubmitting || hasAccepted}
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

        {formError ? (
          <p className="mt-5 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
            {formError}
          </p>
        ) : null}

        <Button
          className="mt-8 w-full"
          type="submit"
          disabled={!token || isSubmitting || hasAccepted}
        >
          {isSubmitting || hasAccepted ? "A criar acesso" : "Criar acesso"}
        </Button>
      </form>
    </main>
  );
}
