"use client";

import { Plus, Search } from "lucide-react";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { useCustomers } from "../../hooks/customer-hooks";
import { useEstimates } from "../../hooks/estimate-hooks";
import {
  estimateStatusLabels,
  estimateStatusValues,
  type EstimateStatus,
} from "../../types/estimates";
import { ErrorState, LoadingState } from "../DashboardState";
import { inputClassName } from "../FormControls";
import { PageHeader } from "../PageHeader";
import { EmptyEstimates } from "./EmptyEstimates";
import {
  EstimateTable,
  type EstimateSortKey,
  type SortDirection,
} from "./EstimateTable";

export function EstimatesPage() {
  const router = useRouter();
  const [statusFilter, setStatusFilter] = useState<EstimateStatus | "">("");
  const [customerFilter, setCustomerFilter] = useState<string>("");
  const [search, setSearch] = useState("");
  const [sortKey, setSortKey] = useState<EstimateSortKey>("issueDate");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");

  const estimatesQuery = useEstimates({
    status: statusFilter || null,
    customerId: customerFilter || null,
  });
  const customersQuery = useCustomers();

  const customerNameById = useMemo(() => {
    const map = new Map<string, string>();
    for (const customer of customersQuery.data ?? []) {
      map.set(customer.id, customer.name);
    }
    return map;
  }, [customersQuery.data]);

  const filteredEstimates = useMemo(() => {
    const estimates = estimatesQuery.data ?? [];
    const term = search.trim().toLowerCase();

    if (!term) {
      return estimates;
    }

    return estimates.filter((estimate) => {
      const customerName = customerNameById.get(estimate.customerId) ?? "";
      return (
        estimate.number.toLowerCase().includes(term) ||
        estimate.title.toLowerCase().includes(term) ||
        customerName.toLowerCase().includes(term)
      );
    });
  }, [customerNameById, estimatesQuery.data, search]);

  function handleSortChange(key: EstimateSortKey) {
    if (key === sortKey) {
      setSortDirection((direction) => (direction === "asc" ? "desc" : "asc"));
      return;
    }
    setSortKey(key);
    setSortDirection("asc");
  }

  if (estimatesQuery.isLoading) {
    return <LoadingState label="A carregar orçamentos" />;
  }

  if (estimatesQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar os orçamentos"
        description="A listagem consome o endpoint autenticado /estimates."
        onRetry={() => void estimatesQuery.refetch()}
      />
    );
  }

  const hasAnyEstimate = (estimatesQuery.data ?? []).length > 0;

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Orçamentos"
        title="Gerir orçamentos"
        description="Crie e acompanhe orçamentos. Números, totais e IVA são calculados e mantidos pelo backend."
        action={
          <Button
            type="button"
            onClick={() => router.push("/dashboard/estimates/new")}
          >
            <Plus size={16} aria-hidden="true" />
            Novo orçamento
          </Button>
        }
      />

      {!hasAnyEstimate ? (
        <EmptyEstimates onCreate={() => router.push("/dashboard/estimates/new")} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="relative block">
              <Search
                size={16}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]"
                aria-hidden="true"
              />
              <input
                type="search"
                className={`${inputClassName} pl-11`}
                placeholder="Pesquisar por número, título ou cliente"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                aria-label="Pesquisar orçamentos"
              />
            </label>

            <select
              className={inputClassName}
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value as EstimateStatus | "")
              }
              aria-label="Filtrar por status"
            >
              <option value="">Todos os status</option>
              {estimateStatusValues.map((status) => (
                <option key={status} value={status}>
                  {estimateStatusLabels[status]}
                </option>
              ))}
            </select>

            <select
              className={inputClassName}
              value={customerFilter}
              onChange={(event) => setCustomerFilter(event.target.value)}
              aria-label="Filtrar por cliente"
            >
              <option value="">Todos os clientes</option>
              {(customersQuery.data ?? []).map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.name}
                </option>
              ))}
            </select>
          </div>

          {filteredEstimates.length === 0 ? (
            <div className="border border-border bg-surface p-8 text-center text-sm text-[var(--muted-foreground)]">
              Nenhum orçamento corresponde aos filtros aplicados.
            </div>
          ) : (
            <EstimateTable
              estimates={filteredEstimates}
              customerNameById={customerNameById}
              sortKey={sortKey}
              sortDirection={sortDirection}
              onSortChange={handleSortChange}
            />
          )}
        </>
      )}
    </div>
  );
}
