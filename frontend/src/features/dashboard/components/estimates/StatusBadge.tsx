import { cn } from "@/lib/utils/cn";
import { estimateStatusLabels, type EstimateStatus } from "../../types/estimates";

const statusStyles: Record<EstimateStatus, string> = {
  DRAFT: "border-[var(--muted-foreground)] text-[var(--muted-foreground)]",
  SENT: "border-primary text-primary",
  APPROVED: "border-success text-success",
  REJECTED: "border-error text-error",
  EXPIRED: "border-warning text-warning",
  CANCELLED: "border-error text-error",
  COMPLETED: "border-success text-success",
};

export function StatusBadge({ status }: { status: EstimateStatus }) {
  return (
    <span
      className={cn(
        "inline-flex min-h-7 items-center border px-3 text-xs font-bold uppercase tracking-[0.12em]",
        statusStyles[status],
      )}
    >
      {estimateStatusLabels[status]}
    </span>
  );
}
