import { cn } from "@/lib/utils/cn";

type TotalsCardProps = {
  label: string;
  value: string;
  emphasis?: boolean;
};

export function TotalsCard({ label, value, emphasis }: TotalsCardProps) {
  return (
    <div
      className={cn(
        "border border-border bg-background p-4",
        emphasis && "border-primary bg-surface",
      )}
    >
      <p className="m-0 text-xs font-bold uppercase tracking-[0.12em] text-[var(--muted-foreground)]">
        {label}
      </p>
      <p
        className={cn(
          "m-0 mt-2 font-display text-xl font-bold",
          emphasis && "text-primary",
        )}
      >
        {value}
      </p>
    </div>
  );
}
