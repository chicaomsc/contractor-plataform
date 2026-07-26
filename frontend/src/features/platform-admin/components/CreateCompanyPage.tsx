"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { ApiError } from "@/lib/api/errors";
import { createCompany } from "../api/platform-admin-api";
import { platformAdminQueryKeys } from "../api/query-keys";
import {
  createCompanySchema,
  type CompanyOnboardingResponse,
  type CreateCompanyInput,
} from "../types/admin";
import { AdminError } from "./AdminPrimitives";
import { InviteLinkPanel } from "./InviteLinkPanel";

export function CreateCompanyPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [created, setCreated] = useState<CompanyOnboardingResponse | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateCompanyInput>({
    resolver: zodResolver(createCompanySchema),
    defaultValues: {
      companyName: "",
      slug: "",
      country: "PT",
      tradeName: "",
      ownerName: "",
      ownerEmail: "",
    },
  });

  const mutation = useMutation({
    mutationFn: (values: CreateCompanyInput) =>
      createCompany(accessToken ?? "", values),
    onSuccess: (response) => {
      setCreated(response);
      queryClient.invalidateQueries({ queryKey: platformAdminQueryKeys.all });
    },
  });

  return (
    <div className="space-y-6">
      <Link
        href="/admin/companies"
        className="inline-flex items-center gap-2 text-sm font-semibold no-underline"
      >
        <ArrowLeft size={16} aria-hidden="true" />
        Voltar para empresas
      </Link>

      <header>
        <h1 className="m-0 font-display text-3xl font-bold">Nova empresa</h1>
        <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
          Cria Company, OWNER pendente e convite numa única operação.
        </p>
      </header>

      {created ? (
        <section className="space-y-4 border border-success bg-surface p-5">
          <div>
            <h2 className="m-0 text-xl font-bold">Company criada</h2>
            <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
              {created.company.name} foi criada com o OWNER pendente {created.owner.email}.
            </p>
          </div>
          <InviteLinkPanel
            token={created.invite.token}
            expiresAt={created.invite.expiresAt}
          />
          <Link
            href={`/admin/companies/${created.company.id}`}
            className="inline-flex text-sm font-semibold no-underline"
          >
            Abrir detalhe da company
          </Link>
        </section>
      ) : null}

      <form
        className="grid gap-5 border border-border bg-surface p-5 md:grid-cols-2"
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
        noValidate
      >
        <label className="block space-y-2">
          <span className="text-sm font-semibold">Nome da empresa</span>
          <input
            className="min-h-12 w-full border border-border bg-background px-4"
            {...register("companyName")}
          />
          {errors.companyName ? (
            <span className="text-sm font-semibold text-error">
              {errors.companyName.message}
            </span>
          ) : null}
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold">Slug</span>
          <input
            className="min-h-12 w-full border border-border bg-background px-4"
            placeholder="opcional"
            {...register("slug")}
          />
          {errors.slug ? (
            <span className="text-sm font-semibold text-error">
              {errors.slug.message}
            </span>
          ) : null}
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold">País</span>
          <input
            className="min-h-12 w-full border border-border bg-background px-4 uppercase"
            maxLength={2}
            {...register("country")}
          />
          {errors.country ? (
            <span className="text-sm font-semibold text-error">
              {errors.country.message}
            </span>
          ) : null}
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold">Nome comercial</span>
          <input
            className="min-h-12 w-full border border-border bg-background px-4"
            placeholder="opcional"
            {...register("tradeName")}
          />
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold">Responsável</span>
          <input
            className="min-h-12 w-full border border-border bg-background px-4"
            {...register("ownerName")}
          />
          {errors.ownerName ? (
            <span className="text-sm font-semibold text-error">
              {errors.ownerName.message}
            </span>
          ) : null}
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold">Email do owner</span>
          <input
            type="email"
            className="min-h-12 w-full border border-border bg-background px-4"
            {...register("ownerEmail")}
          />
          {errors.ownerEmail ? (
            <span className="text-sm font-semibold text-error">
              {errors.ownerEmail.message}
            </span>
          ) : null}
        </label>

        <div className="md:col-span-2">
          {mutation.isError ? (
            <AdminError
              message={
                mutation.error instanceof ApiError
                  ? mutation.error.message
                  : "Não foi possível criar a company."
              }
            />
          ) : null}
          <Button className="mt-4" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "A criar" : "Criar Company e OWNER"}
          </Button>
        </div>
      </form>
    </div>
  );
}
