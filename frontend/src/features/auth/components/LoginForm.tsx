"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "../hooks/auth-context";
import {
  loginFormSchema,
  type LoginFormValues,
} from "../types/auth";

export function LoginForm() {
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
      await login(values);
      router.replace(searchParams.get("next") ?? "/dashboard");
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
      className="w-full max-w-md border border-border bg-surface p-6 shadow-sm md:p-8"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <div className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          Dashboard
        </p>
        <h1 className="m-0 font-display text-3xl font-bold">
          Entrar na área administrativa
        </h1>
        <p className="m-0 text-sm text-[var(--muted-foreground)]">
          Use a conta criada no backend da plataforma.
        </p>
      </div>

      <div className="mt-8 space-y-5">
        <label className="block space-y-2">
          <span className="text-sm font-semibold">Email</span>
          <input
            type="email"
            autoComplete="email"
            className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary"
            {...register("email")}
          />
          {errors.email ? (
            <span className="block text-sm font-semibold text-error">
              {errors.email.message}
            </span>
          ) : null}
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold">Password</span>
          <input
            type="password"
            autoComplete="current-password"
            className="min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary"
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
        <p className="mt-5 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
          {formError}
        </p>
      ) : null}

      <Button className="mt-8 w-full" type="submit" disabled={isSubmitting}>
        {isSubmitting ? "A entrar" : "Entrar"}
      </Button>
    </form>
  );
}
