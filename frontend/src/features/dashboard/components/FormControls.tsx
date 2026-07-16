import type { ReactNode } from "react";
import type { FieldError } from "react-hook-form";

type FieldProps = {
  label: string;
  error?: FieldError;
  children: ReactNode;
  hint?: string;
};

export function Field({ label, error, children, hint }: FieldProps) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-semibold">{label}</span>
      {children}
      {hint ? (
        <span className="block text-xs text-[var(--muted-foreground)]">
          {hint}
        </span>
      ) : null}
      {error ? (
        <span className="block text-sm font-semibold text-error">
          {error.message}
        </span>
      ) : null}
    </label>
  );
}

export const inputClassName =
  "min-h-12 w-full border border-border bg-background px-4 text-base outline-none transition-colors focus:border-primary disabled:opacity-60";

export const textareaClassName =
  "min-h-32 w-full resize-y border border-border bg-background px-4 py-3 text-base outline-none transition-colors focus:border-primary disabled:opacity-60";
