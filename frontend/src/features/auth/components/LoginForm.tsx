"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "../hooks/auth-context";
import {
  loginFormSchema,
  type AuthResponse,
  type LoginFormValues,
} from "../types/auth";

type LoginFormProps = {
  variant?: "owner" | "admin";
};

function getRedirectPath(auth: AuthResponse, requestedNext: string | null) {
  if (auth.user.role === "SUPER_ADMIN") {
    return requestedNext?.startsWith("/admin") ? requestedNext : "/admin";
  }

  return requestedNext?.startsWith("/dashboard") ? requestedNext : "/dashboard";
}

export function LoginForm({ variant = "owner" }: LoginFormProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginFormSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    setFormError(null);

    try {
      const auth = await login(values);
      router.replace(getRedirectPath(auth, searchParams.get("next")));
    } catch (error) {
      setFormError(
        error instanceof ApiError
          ? error.message
          : "Não foi possível iniciar sessão.",
      );
    }
  }

  return (
    <form
      className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8 rounded-xl"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <div className="space-y-2 text-center">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          {variant === "admin" ? "Platform Admin" : "Dashboard"}
        </p>
        <h1 className="m-0 font-display text-3xl font-bold">
          {variant === "admin"
            ? "administração da plataforma"
            : "Área administrativa"}
        </h1>
        <p className="m-0 text-sm text-[var(--muted-foreground)]">
          {variant === "admin"
            ? "Acesso restrito a contas SUPER_ADMIN."
            : "Acesse o painel para gerenciar sua empresa."}
        </p>
      </div>

      <div className="mt-8 space-y-5">
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

        <label className="block space-y-2">
          <span className="text-sm font-semibold">Senha</span>
          <input
            type="password"
            autoComplete="current-password"
            className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary rounded-lg"
            {...register("password")}
          />
          {errors.password ? (
            <span className="block text-sm font-semibold text-error">
              {errors.password.message}
            </span>
          ) : null}
        </label>
      </div>

      {formError ? (
        <p className="mt-5 border border-error bg-background px-4 py-3 text-sm font-semibold text-error text-center rounded-lg">
          {formError}
        </p>
      ) : null}

      <div className="text-right mt-4">
        <Link
          href={
            variant === "admin"
              ? "/forgot-password?variant=admin"
              : "/forgot-password"
          }
          className=" inline-flex text-sm font-semibold text-primary no-underline hover:underline"
        >
          Esqueci minha senha
        </Link>
      </div>

      <Button
        className="mt-6 w-full rounded-lg border shadow-sm"
        type="submit"
        disabled={isSubmitting}
      >
        {isSubmitting ? "A entrar" : "Entrar"}
      </Button>
    </form>
  );
}
