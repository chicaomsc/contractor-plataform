import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { ServiceDto } from "../../types/admin";

type DeleteDialogProps = {
  service: ServiceDto;
  isDeleting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

export function DeleteDialog({
  service,
  isDeleting,
  onCancel,
  onConfirm,
}: DeleteDialogProps) {
  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/45 px-4"
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-service-title"
        className="w-full max-w-lg border border-border bg-surface p-6 shadow-sm"
      >
        <div className="flex gap-4">
          <AlertTriangle
            size={24}
            className="mt-1 shrink-0 text-error"
            aria-hidden="true"
          />
          <div>
            <h2
              id="delete-service-title"
              className="m-0 font-display text-2xl font-semibold"
            >
              Excluir serviço
            </h2>
            <p className="m-0 mt-3 text-sm text-[var(--muted-foreground)]">
              Confirme a exclusão de <strong>{service.name}</strong>. Esta
              ação remove o serviço da área administrativa e da landing pública.
            </p>
          </div>
        </div>
        <div className="mt-8 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="secondary"
            onClick={onCancel}
            disabled={isDeleting}
          >
            Cancelar
          </Button>
          <Button type="button" onClick={onConfirm} disabled={isDeleting}>
            {isDeleting ? "A excluir" : "Excluir"}
          </Button>
        </div>
      </div>
    </div>
  );
}
