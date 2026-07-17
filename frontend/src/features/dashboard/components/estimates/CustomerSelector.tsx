"use client";

import { Check, Search, UserPlus } from "lucide-react";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils/cn";
import { useCustomers } from "../../hooks/customer-hooks";
import type { CustomerDto } from "../../types/customers";
import { ErrorState, LoadingState } from "../DashboardState";
import { inputClassName } from "../FormControls";

type CustomerSelectorProps = {
  selectedCustomerId: string | null;
  onSelect: (customer: CustomerDto) => void;
  onCreateNew: () => void;
};

export function CustomerSelector({
  selectedCustomerId,
  onSelect,
  onCreateNew,
}: CustomerSelectorProps) {
  const customersQuery = useCustomers();
  const [search, setSearch] = useState("");

  const customers = useMemo(() => {
    const all = (customersQuery.data ?? []).filter((customer) => customer.active);
    const term = search.trim().toLowerCase();
    if (!term) {
      return all;
    }
    return all.filter(
      (customer) =>
        customer.name.toLowerCase().includes(term) ||
        (customer.email ?? "").toLowerCase().includes(term) ||
        (customer.phone ?? "").toLowerCase().includes(term),
    );
  }, [customersQuery.data, search]);

  if (customersQuery.isLoading) {
    return <LoadingState label="A carregar clientes" />;
  }

  if (customersQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar os clientes"
        description="A seleção consome o endpoint autenticado /customers."
        onRetry={() => void customersQuery.refetch()}
      />
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <label className="relative block flex-1">
          <Search
            size={16}
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]"
            aria-hidden="true"
          />
          <input
            type="search"
            className={`${inputClassName} pl-11`}
            placeholder="Pesquisar cliente por nome, email ou telefone"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Pesquisar cliente"
          />
        </label>
        <Button type="button" variant="secondary" onClick={onCreateNew}>
          <UserPlus size={16} aria-hidden="true" />
          Criar cliente
        </Button>
      </div>

      {customers.length === 0 ? (
        <div className="border border-border bg-surface p-6 text-center text-sm text-[var(--muted-foreground)]">
          Nenhum cliente ativo encontrado. Crie um novo cliente para continuar.
        </div>
      ) : (
        <ul className="m-0 max-h-[420px] list-none space-y-2 overflow-y-auto p-0">
          {customers.map((customer) => {
            const isSelected = customer.id === selectedCustomerId;

            return (
              <li key={customer.id}>
                <button
                  type="button"
                  onClick={() => onSelect(customer)}
                  className={cn(
                    "flex w-full items-center justify-between gap-4 border border-border bg-surface px-4 py-3 text-left transition-colors hover:border-primary",
                    isSelected && "border-primary bg-background",
                  )}
                >
                  <span>
                    <span className="block font-semibold">{customer.name}</span>
                    <span className="block text-sm text-[var(--muted-foreground)]">
                      {[customer.email, customer.phone].filter(Boolean).join(" · ") ||
                        "Sem contacto registado"}
                    </span>
                  </span>
                  {isSelected ? (
                    <Check size={20} className="shrink-0 text-primary" aria-hidden="true" />
                  ) : null}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
