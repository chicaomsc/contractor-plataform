import type { ServiceDto } from "../../types/admin";
import { ServiceCard } from "./ServiceCard";

type ServiceListProps = {
  services: ServiceDto[];
  isBusy: boolean;
  onEdit: (service: ServiceDto) => void;
  onToggleStatus: (service: ServiceDto) => void;
  onMoveUp: (service: ServiceDto) => void;
  onMoveDown: (service: ServiceDto) => void;
  onDelete: (service: ServiceDto) => void;
};

export function sortServices(services: ServiceDto[]) {
  return [...services].sort((a, b) => {
    if (a.displayOrder !== b.displayOrder) {
      return a.displayOrder - b.displayOrder;
    }

    return a.name.localeCompare(b.name, "pt");
  });
}

export function ServiceList({
  services,
  isBusy,
  onEdit,
  onToggleStatus,
  onMoveUp,
  onMoveDown,
  onDelete,
}: ServiceListProps) {
  const sortedServices = sortServices(services);

  return (
    <div className="space-y-3">
      {sortedServices.map((service, index) => (
        <ServiceCard
          key={service.id}
          service={service}
          isFirst={index === 0}
          isLast={index === sortedServices.length - 1}
          isBusy={isBusy}
          onEdit={onEdit}
          onToggleStatus={onToggleStatus}
          onMoveUp={onMoveUp}
          onMoveDown={onMoveDown}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
}
