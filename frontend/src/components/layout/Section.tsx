import type { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type SectionProps = {
  children: ReactNode;
  id?: string;
  variant?: "default" | "muted" | "dark";
  labelledBy?: string;
  className?: string;
};

export function Section({
  children,
  id,
  variant = "default",
  labelledBy,
  className,
}: SectionProps) {
  return (
    <section
      id={id}
      aria-labelledby={labelledBy}
      className={cn(
        "py-16 md:py-20 lg:py-24",
        variant === "default" && "bg-background text-foreground",
        variant === "muted" && "bg-surface-muted text-foreground",
        variant === "dark" &&
          "bg-[var(--surface-dark)] text-[var(--surface-dark-fg)]",
        className,
      )}
    >
      {children}
    </section>
  );
}
