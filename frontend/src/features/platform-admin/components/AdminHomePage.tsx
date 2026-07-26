"use client";

import { ArrowRight, Building2, Plus } from "lucide-react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { ApiError } from "@/lib/api/errors";
import { listCompanies } from "../api/platform-admin-api";
import { platformAdminQueryKeys } from "../api/query-keys";
import { AdminError, AdminLoading, StatusBadge, formatDate } from "./AdminPrimitives";

export function AdminHomePage() {
  const { accessToken } = useAuth();
  const query = useQuery({
    queryKey: platformAdminQueryKeys.companies({ page: 0, size: 5 }),
    queryFn: () => listCompanies(accessToken ?? "", { page: 0, size: 5 }),
    enabled: Boolean(accessToken),
  });

  if (query.isLoading) return <AdminLoading label="A carregar empresas" />;

  if (query.isError) {
    return (
      <AdminError
        message={
          query.error instanceof ApiError
            ? query.error.message
            : "Não foi possível carregar a administração."
        }
      />
    );
  }

  const companies = query.data?.content ?? [];

  return (
    <div className="space-y-8">
      <header className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div>
          <p className="m-0 text-sm font-semibold uppercase tracking-[0.18em] text-primary">
            Visão geral
          </p>
          <h1 className="m-0 mt-2 font-display text-3xl font-bold">
            Administração da plataforma
          </h1>
          <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
            Provisionamento e operação de empresas multi-tenant.
          </p>
        </div>
        <Link href="/admin/companies/new" className="no-underline">
          <Button type="button">
            <Plus size={16} aria-hidden="true" />
            Nova empresa
          </Button>
        </Link>
      </header>

      <section className="grid gap-4 md:grid-cols-[260px_minmax(0,1fr)]">
        <div className="border border-border bg-surface p-5">
          <div className="flex items-center gap-3">
            <Building2 size={20} aria-hidden="true" />
            <p className="m-0 text-sm font-semibold">Total de empresas</p>
          </div>
          <p className="m-0 mt-4 font-display text-4xl font-bold">
            {query.data?.totalElements ?? 0}
          </p>
        </div>

        <div className="border border-border bg-surface">
          <div className="flex items-center justify-between border-b border-border px-5 py-4">
            <h2 className="m-0 text-lg font-bold">Empresas recentes</h2>
            <Link
              href="/admin/companies"
              className="inline-flex items-center gap-2 text-sm font-semibold no-underline"
            >
              Ver todas
              <ArrowRight size={15} aria-hidden="true" />
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase text-[var(--muted-foreground)]">
                  <th className="px-5 py-3">Empresa</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Criada em</th>
                </tr>
              </thead>
              <tbody>
                {companies.map((company) => (
                  <tr key={company.id} className="border-b border-border">
                    <td className="px-5 py-4">
                      <Link
                        href={`/admin/companies/${company.id}`}
                        className="font-semibold no-underline"
                      >
                        {company.name}
                      </Link>
                      <p className="m-0 text-xs text-[var(--muted-foreground)]">
                        {company.slug}
                      </p>
                    </td>
                    <td className="px-5 py-4">
                      <StatusBadge status={company.status} />
                    </td>
                    <td className="px-5 py-4">{formatDate(company.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}
