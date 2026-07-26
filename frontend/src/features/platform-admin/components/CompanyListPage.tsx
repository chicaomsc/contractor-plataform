"use client";

import { Eye, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { ApiError } from "@/lib/api/errors";
import { listCompanies } from "../api/platform-admin-api";
import { platformAdminQueryKeys } from "../api/query-keys";
import { AdminError, AdminLoading, StatusBadge, formatDate } from "./AdminPrimitives";

export function CompanyListPage() {
  const { accessToken } = useAuth();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"ACTIVE" | "INACTIVE" | "">("");
  const [page, setPage] = useState(0);
  const params = useMemo(() => ({ search, status, page, size: 10 }), [page, search, status]);
  const query = useQuery({
    queryKey: platformAdminQueryKeys.companies(params),
    queryFn: () => listCompanies(accessToken ?? "", params),
    enabled: Boolean(accessToken),
  });

  return (
    <div className="space-y-6">
      <header className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div>
          <h1 className="m-0 font-display text-3xl font-bold">Empresas</h1>
          <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
            Lista paginada de companies provisionadas na plataforma.
          </p>
        </div>
        <Link href="/admin/companies/new" className="no-underline">
          <Button type="button">
            <Plus size={16} aria-hidden="true" />
            Nova empresa
          </Button>
        </Link>
      </header>

      <form
        className="grid gap-3 border border-border bg-surface p-4 md:grid-cols-[minmax(0,1fr)_180px_auto]"
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
        }}
      >
        <label className="block">
          <span className="sr-only">Buscar empresas</span>
          <div className="flex min-h-12 items-center gap-2 border border-border bg-background px-3">
            <Search size={16} aria-hidden="true" />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Buscar por nome ou slug"
              className="min-w-0 flex-1 bg-transparent text-sm outline-none"
            />
          </div>
        </label>
        <label className="block">
          <span className="sr-only">Filtrar status</span>
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as "ACTIVE" | "INACTIVE" | "");
              setPage(0);
            }}
            className="min-h-12 w-full border border-border bg-background px-3 text-sm"
          >
            <option value="">Todos os status</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </label>
        <Button type="submit" variant="secondary">
          Aplicar
        </Button>
      </form>

      {query.isLoading ? <AdminLoading label="A carregar empresas" /> : null}
      {query.isError ? (
        <AdminError
          message={
            query.error instanceof ApiError
              ? query.error.message
              : "Não foi possível listar empresas."
          }
        />
      ) : null}

      {query.data ? (
        <div className="border border-border bg-surface">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase text-[var(--muted-foreground)]">
                  <th className="px-5 py-3">Nome</th>
                  <th className="px-5 py-3">Slug</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Owner</th>
                  <th className="px-5 py-3">Criada em</th>
                  <th className="px-5 py-3 text-right">Ação</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((company) => (
                  <tr key={company.id} className="border-b border-border">
                    <td className="px-5 py-4 font-semibold">{company.name}</td>
                    <td className="px-5 py-4">{company.slug}</td>
                    <td className="px-5 py-4">
                      <StatusBadge status={company.status} />
                    </td>
                    <td className="px-5 py-4">{company.ownerEmail ?? "-"}</td>
                    <td className="px-5 py-4">{formatDate(company.createdAt)}</td>
                    <td className="px-5 py-4 text-right">
                      <Link
                        href={`/admin/companies/${company.id}`}
                        className="inline-flex items-center gap-2 font-semibold no-underline"
                      >
                        <Eye size={16} aria-hidden="true" />
                        Visualizar
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex flex-col justify-between gap-3 border-t border-border px-5 py-4 text-sm md:flex-row md:items-center">
            <span>
              Página {query.data.number + 1} de {Math.max(query.data.totalPages, 1)} · {query.data.totalElements} empresas
            </span>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={page <= 0}
                onClick={() => setPage((current) => Math.max(current - 1, 0))}
              >
                Anterior
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={page + 1 >= query.data.totalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                Próxima
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
