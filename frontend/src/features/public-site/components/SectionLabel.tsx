import { cn } from "@/lib/utils/cn";

type SectionLabelProps = {
  children: string;
  tone?: "default" | "inverse";
};

export function SectionLabel({
  children,
  tone = "default",
}: SectionLabelProps) {
  return (
    <p
      className={cn(
        "m-0 text-xs font-semibold uppercase tracking-[0.12em]",
        tone === "inverse" ? "text-[var(--primary-on-dark)]" : "text-primary",
      )}
    >
      {children}
    </p>
  );
}
