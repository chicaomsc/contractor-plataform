import {
  ArrowDown,
  ArrowUp,
  Pencil,
  Power,
  PowerOff,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { ServiceDto } from "../../types/admin";
import { StatusBadge } from "./StatusBadge";

type ServiceCardProps = {
  service: ServiceDto;
  isFirst: boolean;
  isLast: boolean;
  isBusy: boolean;
  onEdit: (service: ServiceDto) => void;
  onToggleStatus: (service: ServiceDto) => void;
  onMoveUp: (service: ServiceDto) => void;
  onMoveDown: (service: ServiceDto) => void;
  onDelete: (service: ServiceDto) => void;
};

export function ServiceCard({
  service,
  isFirst,
  isLast,
  isBusy,
  onEdit,
  onToggleStatus,
  onMoveUp,
  onMoveDown,
  onDelete,
}: ServiceCardProps) {
  return (
    <article className="border border-border bg-surface p-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <span className="font-display text-sm font-bold text-primary">
              {service.displayOrder.toString().padStart(2, "0")}
            </span>
            <StatusBadge active={service.active} />
            {service.icon ? (
              <span className="text-xs font-semibold text-[var(--muted-foreground)]">
                {service.icon}
              </span>
            ) : null}
          </div>
          <h2 className="m-0 mt-4 font-display text-2xl font-semibold leading-tight">
            {service.name}
          </h2>
          <p className="m-0 mt-2 max-w-3xl text-sm text-[var(--muted-foreground)]">
            {service.shortDescription ??
              service.description ??
              "Sem descrição configurada."}
          </p>
        </div>

        <div className="flex flex-wrap gap-2 lg:justify-end">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onMoveUp(service)}
            disabled={isFirst || isBusy}
            aria-label={`Mover ${service.name} para cima`}
          >
            <ArrowUp size={16} aria-hidden="true" />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onMoveDown(service)}
            disabled={isLast || isBusy}
            aria-label={`Mover ${service.name} para baixo`}
          >
            <ArrowDown size={16} aria-hidden="true" />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onToggleStatus(service)}
            disabled={isBusy}
          >
            {service.active ? (
              <PowerOff size={16} aria-hidden="true" />
            ) : (
              <Power size={16} aria-hidden="true" />
            )}
            {service.active ? "Desativar" : "Ativar"}
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onEdit(service)}
            disabled={isBusy}
          >
            <Pencil size={16} aria-hidden="true" />
            Editar
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onDelete(service)}
            disabled={isBusy}
          >
            <Trash2 size={16} aria-hidden="true" />
            Excluir
          </Button>
        </div>
      </div>
    </article>
  );
}
