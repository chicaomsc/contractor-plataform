export function ImageStatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={
        active
          ? "inline-flex min-h-7 items-center border border-success px-3 text-xs font-bold uppercase tracking-[0.12em] text-success"
          : "inline-flex min-h-7 items-center border border-border px-3 text-xs font-bold uppercase tracking-[0.12em] text-[var(--muted-foreground)]"
      }
    >
      {active ? "Ativa" : "Inativa"}
    </span>
  );
}
