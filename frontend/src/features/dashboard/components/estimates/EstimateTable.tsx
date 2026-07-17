"use client";

import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/utils/cn";
import type { EstimateSummaryDto } from "../../types/estimates";
import { formatDate, formatMoney } from "../../utils/money";
import { StatusBadge } from "./StatusBadge";

export type EstimateSortKey = "number" | "customer" | "status" | "total" | "issueDate";
export type SortDirection = "asc" | "desc";

type EstimateTableProps = {
  estimates: EstimateSummaryDto[];
  customerNameById: Map<string, string>;
  sortKey: EstimateSortKey;
  sortDirection: SortDirection;
  onSortChange: (key: EstimateSortKey) => void;
};

export function sortEstimates(
  estimates: EstimateSummaryDto[],
  sortKey: EstimateSortKey,
  direction: SortDirection,
  customerNameById: Map<string, string>,
): EstimateSummaryDto[] {
  const factor = direction === "asc" ? 1 : -1;

  return [...estimates].sort((a, b) => {
    switch (sortKey) {
      case "number":
        return a.number.localeCompare(b.number, "pt") * factor;
      case "customer": {
        const nameA = customerNameById.get(a.customerId) ?? "";
        const nameB = customerNameById.get(b.customerId) ?? "";
        return nameA.localeCompare(nameB, "pt") * factor;
      }
      case "status":
        return a.status.localeCompare(b.status, "pt") * factor;
      case "total":
        return (a.total - b.total) * factor;
      case "issueDate":
        return (Date.parse(a.issueDate) - Date.parse(b.issueDate)) * factor;
      default:
        return 0;
    }
  });
}

const columns: Array<{ key: EstimateSortKey; label: string }> = [
  { key: "number", label: "Número" },
  { key: "customer", label: "Cliente" },
  { key: "status", label: "Status" },
  { key: "total", label: "Total" },
  { key: "issueDate", label: "Data" },
];

export function EstimateTable({
  estimates,
  customerNameById,
  sortKey,
  sortDirection,
  onSortChange,
}: EstimateTableProps) {
  const router = useRouter();
  const sorted = sortEstimates(estimates, sortKey, sortDirection, customerNameById);

  return (
    <div className="overflow-x-auto border border-border bg-surface">
      <table className="w-full min-w-[720px] border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-border">
            {columns.map((column) => {
              const isActive = sortKey === column.key;
              const Icon = isActive
                ? sortDirection === "asc"
                  ? ArrowUp
                  : ArrowDown
                : ArrowUpDown;

              return (
                <th key={column.key} className="p-0">
                  <button
                    type="button"
                    onClick={() => onSortChange(column.key)}
                    className={cn(
                      "flex min-h-11 w-full items-center gap-2 px-4 text-xs font-bold uppercase tracking-[0.1em] text-[var(--muted-foreground)] transition-colors hover:text-foreground",
                      isActive && "text-foreground",
                    )}
                  >
                    {column.label}
                    <Icon size={14} aria-hidden="true" />
                  </button>
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {sorted.map((estimate) => (
            <tr
              key={estimate.id}
              tabIndex={0}
              role="link"
              aria-label={`Abrir orçamento ${estimate.number}`}
              onClick={() => router.push(`/dashboard/estimates/${estimate.id}`)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  router.push(`/dashboard/estimates/${estimate.id}`);
                }
              }}
              className="cursor-pointer border-b border-border last:border-b-0 transition-colors hover:bg-surface-muted focus:bg-surface-muted focus:outline-none"
            >
              <td className="px-4 py-4 font-semibold">{estimate.number}</td>
              <td className="px-4 py-4">
                {customerNameById.get(estimate.customerId) ?? "—"}
              </td>
              <td className="px-4 py-4">
                <StatusBadge status={estimate.status} />
              </td>
              <td className="px-4 py-4 font-semibold">
                {formatMoney(estimate.total, estimate.currency)}
              </td>
              <td className="px-4 py-4 text-[var(--muted-foreground)]">
                {formatDate(estimate.issueDate)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
