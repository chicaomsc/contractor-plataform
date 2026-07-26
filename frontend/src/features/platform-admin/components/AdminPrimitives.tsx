import { cn } from "@/lib/utils/cn";

export function StatusBadge({ status }: { status: string }) {
  const isActive = status === "ACTIVE";

  return (
    <span
      className={cn(
        "inline-flex min-h-7 items-center border px-2.5 text-xs font-bold uppercase",
        isActive
          ? "border-success/30 bg-success/10 text-success"
          : "border-warning/30 bg-warning/10 text-warning",
      )}
    >
      {status}
    </span>
  );
}

export function AdminError({ message }: { message: string }) {
  return (
    <p
      role="alert"
      className="border border-error bg-surface px-4 py-3 text-sm font-semibold text-error"
    >
      {message}
    </p>
  );
}

export function AdminLoading({ label = "A carregar" }: { label?: string }) {
  return (
    <div
      role="status"
      className="border border-border bg-surface px-4 py-6 text-sm font-semibold text-[var(--muted-foreground)]"
    >
      {label}
    </div>
  );
}

export function formatDate(value: string | null | undefined) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("pt-PT", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
