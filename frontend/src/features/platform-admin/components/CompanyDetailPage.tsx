"use client";

import { zodResolver } from "@/features/dashboard/utils/zod-resolver";
import { ArrowLeft, ExternalLink, Plus, RefreshCw, ShieldOff, Trash2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { ApiError } from "@/lib/api/errors";
import { buildTenantLandingUrl } from "@/lib/tenant/tenant-landing-url";
import {
  getCompany,
  inviteOwner,
  reissueInvite,
  revokeInvite,
  updateCompanyStatus,
} from "../api/platform-admin-api";
import { platformAdminQueryKeys } from "../api/query-keys";
import {
  inviteOwnerSchema,
  type InviteOwnerInput,
  type InviteResponse,
  type OwnerInviteResponse,
} from "../types/admin";
import { AdminError, AdminLoading, StatusBadge, formatDate } from "./AdminPrimitives";
import { InviteLinkPanel } from "./InviteLinkPanel";

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError && error.status === 409) {
    return "A operação já não é válida para o estado atual.";
  }

  return error instanceof ApiError ? error.message : fallback;
}

export function CompanyDetailPage({ companyId }: { companyId: string }) {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [inviteResult, setInviteResult] = useState<
    OwnerInviteResponse | InviteResponse | null
  >(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InviteOwnerInput>({
    resolver: zodResolver(inviteOwnerSchema),
    defaultValues: { ownerName: "", ownerEmail: "" },
  });

  const query = useQuery({
    queryKey: platformAdminQueryKeys.company(companyId),
    queryFn: () => getCompany(accessToken ?? "", companyId),
    enabled: Boolean(accessToken),
  });

  const refreshCompany = () => {
    queryClient.invalidateQueries({
      queryKey: platformAdminQueryKeys.company(companyId),
    });
    queryClient.invalidateQueries({ queryKey: platformAdminQueryKeys.all });
  };

  const statusMutation = useMutation({
    mutationFn: (status: "ACTIVE" | "INACTIVE") =>
      updateCompanyStatus(accessToken ?? "", companyId, status),
    onSuccess: refreshCompany,
    onError: (error) =>
      setActionError(getErrorMessage(error, "Não foi possível atualizar status.")),
  });

  const inviteOwnerMutation = useMutation({
    mutationFn: (values: InviteOwnerInput) =>
      inviteOwner(accessToken ?? "", companyId, values),
    onSuccess: (response) => {
      setInviteResult(response);
      reset();
      refreshCompany();
    },
    onError: (error) =>
      setActionError(getErrorMessage(error, "Não foi possível convidar owner.")),
  });

  const reissueMutation = useMutation({
    mutationFn: (ownerId: string) =>
      reissueInvite(accessToken ?? "", companyId, ownerId),
    onSuccess: (response) => {
      setInviteResult(response);
      refreshCompany();
    },
    onError: (error) =>
      setActionError(getErrorMessage(error, "Não foi possível gerar novo convite.")),
  });

  const revokeMutation = useMutation({
    mutationFn: (ownerId: string) =>
      revokeInvite(accessToken ?? "", companyId, ownerId),
    onSuccess: () => {
      setInviteResult(null);
      refreshCompany();
    },
    onError: (error) =>
      setActionError(getErrorMessage(error, "Não foi possível revogar convite.")),
  });

  if (query.isLoading) return <AdminLoading label="A carregar company" />;
  if (query.isError || !query.data) {
    return (
      <AdminError
        message={getErrorMessage(query.error, "Não foi possível carregar company.")}
      />
    );
  }

  const { company, owners } = query.data;
  const nextStatus = company.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
  const landingUrl = buildTenantLandingUrl(company.slug);

  function requestStatusChange() {
    if (
      nextStatus === "INACTIVE" &&
      !window.confirm(
        "Desativar esta Company bloqueia operação dos OWNERs e remove a landing pública do tenant. Continuar?",
      )
    ) {
      return;
    }

    statusMutation.mutate(nextStatus);
  }

  return (
    <div className="space-y-6">
      <Link
        href="/admin/companies"
        className="inline-flex items-center gap-2 text-sm font-semibold no-underline"
      >
        <ArrowLeft size={16} aria-hidden="true" />
        Voltar para empresas
      </Link>

      <header className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="m-0 font-display text-3xl font-bold">{company.name}</h1>
            <StatusBadge status={company.status} />
          </div>
          <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
            {company.slug} · {company.country ?? "-"} · criada em {formatDate(company.createdAt)}
          </p>
        </div>
        <div className="flex flex-col items-stretch gap-2 sm:flex-row sm:items-center">
          <a
            href={landingUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex min-h-12 items-center justify-center gap-2 border-2 border-foreground bg-transparent px-5 py-3 text-sm font-semibold text-foreground no-underline transition-colors hover:bg-foreground hover:text-surface"
          >
            <ExternalLink size={16} aria-hidden="true" />
            Visualizar landing
          </a>
          <Button
            type="button"
            variant={nextStatus === "INACTIVE" ? "secondary" : "primary"}
            onClick={requestStatusChange}
            disabled={statusMutation.isPending}
          >
            <ShieldOff size={16} aria-hidden="true" />
            {nextStatus === "INACTIVE" ? "Desativar" : "Ativar"}
          </Button>
        </div>
      </header>

      {company.status !== "ACTIVE" ? (
        <p className="m-0 text-sm font-semibold text-[var(--muted-foreground)]">
          Esta Company está inativa; a landing pública pode retornar indisponível.
        </p>
      ) : null}

      {actionError ? <AdminError message={actionError} /> : null}

      <section className="grid gap-4 border border-border bg-surface p-5 md:grid-cols-2 lg:grid-cols-4">
        <div>
          <p className="m-0 text-xs font-bold uppercase text-[var(--muted-foreground)]">Nome comercial</p>
          <p className="m-0 mt-1 font-semibold">{company.tradeName ?? "-"}</p>
        </div>
        <div>
          <p className="m-0 text-xs font-bold uppercase text-[var(--muted-foreground)]">Email</p>
          <p className="m-0 mt-1 font-semibold">{company.email ?? "-"}</p>
        </div>
        <div>
          <p className="m-0 text-xs font-bold uppercase text-[var(--muted-foreground)]">Slug</p>
          <p className="m-0 mt-1 font-semibold">{company.slug}</p>
        </div>
        <div>
          <p className="m-0 text-xs font-bold uppercase text-[var(--muted-foreground)]">Status</p>
          <p className="m-0 mt-1 font-semibold">{company.status}</p>
        </div>
      </section>

      {inviteResult ? (
        <InviteLinkPanel
          token={inviteResult.invite.token}
          expiresAt={inviteResult.invite.expiresAt}
        />
      ) : null}

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="border border-border bg-surface">
          <div className="border-b border-border px-5 py-4">
            <h2 className="m-0 text-xl font-bold">Owners</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase text-[var(--muted-foreground)]">
                  <th className="px-5 py-3">Nome</th>
                  <th className="px-5 py-3">Email</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Criado em</th>
                  <th className="px-5 py-3 text-right">Convite</th>
                </tr>
              </thead>
              <tbody>
                {owners.map((owner) => (
                  <tr key={owner.id} className="border-b border-border">
                    <td className="px-5 py-4 font-semibold">{owner.name}</td>
                    <td className="px-5 py-4">{owner.email}</td>
                    <td className="px-5 py-4">
                      <StatusBadge status={owner.status} />
                    </td>
                    <td className="px-5 py-4">{formatDate(owner.createdAt)}</td>
                    <td className="px-5 py-4">
                      {owner.status === "PENDING" ? (
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            disabled={reissueMutation.isPending}
                            onClick={() => {
                              if (window.confirm("Gerar novo convite e revogar o link anterior?")) {
                                reissueMutation.mutate(owner.id);
                              }
                            }}
                          >
                            <RefreshCw size={15} aria-hidden="true" />
                            Gerar novo
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            disabled={revokeMutation.isPending}
                            onClick={() => {
                              if (window.confirm("Revogar o convite atual deste owner?")) {
                                revokeMutation.mutate(owner.id);
                              }
                            }}
                          >
                            <Trash2 size={15} aria-hidden="true" />
                            Revogar
                          </Button>
                        </div>
                      ) : (
                        <span className="block text-right text-xs text-[var(--muted-foreground)]">
                          Sem convite pendente
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <form
          className="space-y-4 border border-border bg-surface p-5"
          onSubmit={handleSubmit((values) => inviteOwnerMutation.mutate(values))}
          noValidate
        >
          <div>
            <h2 className="m-0 text-xl font-bold">Adicionar owner</h2>
            <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
              O owner criado fica PENDING até aceitar o convite.
            </p>
          </div>
          <label className="block space-y-2">
            <span className="text-sm font-semibold">Nome</span>
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
            <span className="text-sm font-semibold">Email</span>
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
          <Button
            type="submit"
            disabled={inviteOwnerMutation.isPending}
            className="w-full"
          >
            <Plus size={16} aria-hidden="true" />
            {inviteOwnerMutation.isPending ? "A criar" : "Criar owner"}
          </Button>
        </form>
      </section>
    </div>
  );
}
