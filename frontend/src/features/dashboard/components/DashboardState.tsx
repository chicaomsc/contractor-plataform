import { AlertCircle, Loader2, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/Button";

type DashboardStateProps = {
  title: string;
  description: string;
  onRetry?: () => void;
};

export function LoadingState({ label = "A carregar" }: { label?: string }) {
  return (
    <div className="flex min-h-[280px] items-center justify-center border border-border bg-surface">
      <div className="inline-flex items-center gap-3 text-sm font-semibold text-[var(--muted-foreground)]">
        <Loader2 size={18} className="animate-spin" aria-hidden="true" />
        {label}
      </div>
    </div>
  );
}

export function ErrorState({
  title,
  description,
  onRetry,
}: DashboardStateProps) {
  return (
    <div className="border border-error bg-surface p-6">
      <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex gap-3">
          <AlertCircle
            size={22}
            className="mt-1 shrink-0 text-error"
            aria-hidden="true"
          />
          <div>
            <h2 className="m-0 font-display text-xl font-semibold">{title}</h2>
            <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
              {description}
            </p>
          </div>
        </div>
        {onRetry ? (
          <Button type="button" variant="secondary" size="sm" onClick={onRetry}>
            <RefreshCw size={16} aria-hidden="true" />
            Tentar novamente
          </Button>
        ) : null}
      </div>
    </div>
  );
}

export function SaveFeedback({
  isError,
  isSuccess,
}: {
  isError: boolean;
  isSuccess: boolean;
}) {
  if (isError) {
    return (
      <p className="m-0 border border-error bg-background px-4 py-3 text-sm font-semibold text-error">
        Não foi possível guardar. Reveja os dados e tente novamente.
      </p>
    );
  }

  if (isSuccess) {
    return (
      <p className="m-0 border border-success bg-background px-4 py-3 text-sm font-semibold text-success">
        Alterações guardadas.
      </p>
    );
  }

  return null;
}
