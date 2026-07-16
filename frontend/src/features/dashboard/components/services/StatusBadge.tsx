import { cn } from "@/lib/utils/cn";

export function StatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={cn(
        "inline-flex min-h-7 items-center px-3 text-xs font-bold uppercase tracking-[0.12em]",
        active
          ? "border border-success text-success"
          : "border border-[var(--muted-foreground)] text-[var(--muted-foreground)]",
      )}
    >
      {active ? "Ativo" : "Inativo"}
    </span>
  );
}
