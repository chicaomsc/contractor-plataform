"use client";

import { Plus } from "lucide-react";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import {
  useCreateService,
  useDeleteService,
  useReorderService,
  useServices,
  useUpdateService,
} from "../../hooks/dashboard-hooks";
import type { ServiceDto, ServiceFormInput } from "../../types/admin";
import { ErrorState, LoadingState, SaveFeedback } from "../DashboardState";
import { PageHeader } from "../PageHeader";
import { DeleteDialog } from "./DeleteDialog";
import { EmptyServices } from "./EmptyServices";
import { ServiceForm } from "./ServiceForm";
import { ServiceList, sortServices } from "./ServiceList";

type FormMode =
  | { type: "closed" }
  | { type: "create" }
  | { type: "edit"; service: ServiceDto };

function toServicePayload(service: ServiceDto): ServiceFormInput {
  return {
    name: service.name,
    shortDescription: service.shortDescription ?? "",
    description: service.description ?? "",
    icon: service.icon ?? "",
    displayOrder: service.displayOrder,
    active: service.active,
  };
}

export function ServicesPage() {
  const servicesQuery = useServices();
  const createMutation = useCreateService();
  const updateMutation = useUpdateService();
  const deleteMutation = useDeleteService();
  const reorderMutation = useReorderService();
  const [formMode, setFormMode] = useState<FormMode>({ type: "closed" });
  const [serviceToDelete, setServiceToDelete] = useState<ServiceDto | null>(
    null,
  );

  const services = useMemo(
    () => sortServices(servicesQuery.data ?? []),
    [servicesQuery.data],
  );
  const nextDisplayOrder =
    services.length > 0
      ? Math.max(...services.map((service) => service.displayOrder)) + 1
      : 0;
  const isMutating =
    createMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending ||
    reorderMutation.isPending;

  async function submitService(values: ServiceFormInput) {
    if (formMode.type === "edit") {
      await updateMutation.mutateAsync({
        serviceId: formMode.service.id,
        payload: values,
      });
      setFormMode({ type: "closed" });
      return;
    }

    await createMutation.mutateAsync(values);
    setFormMode({ type: "closed" });
  }

  async function toggleStatus(service: ServiceDto) {
    await updateMutation.mutateAsync({
      serviceId: service.id,
      payload: {
        ...toServicePayload(service),
        active: !service.active,
      },
    });
  }

  async function moveService(service: ServiceDto, direction: "up" | "down") {
    const currentIndex = services.findIndex((item) => item.id === service.id);
    const targetIndex = direction === "up" ? currentIndex - 1 : currentIndex + 1;
    const target = services[targetIndex];

    if (!target) {
      return;
    }

    await Promise.all([
      reorderMutation.mutateAsync({
        serviceId: service.id,
        displayOrder: target.displayOrder,
      }),
      reorderMutation.mutateAsync({
        serviceId: target.id,
        displayOrder: service.displayOrder,
      }),
    ]);
  }

  async function confirmDelete() {
    if (!serviceToDelete) {
      return;
    }

    await deleteMutation.mutateAsync(serviceToDelete.id);
    setServiceToDelete(null);
  }

  if (servicesQuery.isLoading) {
    return <LoadingState label="A carregar serviços" />;
  }

  if (servicesQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar os serviços"
        description="A listagem consome o endpoint autenticado /services."
        onRetry={() => void servicesQuery.refetch()}
      />
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Serviços"
        title="Gerenciar serviços"
        description="Crie, edite, ordene e publique os serviços que aparecem na landing pública."
        action={
          <Button type="button" onClick={() => setFormMode({ type: "create" })}>
            <Plus size={16} aria-hidden="true" />
            Novo serviço
          </Button>
        }
      />

      <SaveFeedback
        isError={
          createMutation.isError ||
          updateMutation.isError ||
          deleteMutation.isError ||
          reorderMutation.isError
        }
        isSuccess={
          createMutation.isSuccess ||
          updateMutation.isSuccess ||
          deleteMutation.isSuccess ||
          reorderMutation.isSuccess
        }
      />

      {formMode.type !== "closed" ? (
        <ServiceForm
          service={formMode.type === "edit" ? formMode.service : null}
          nextDisplayOrder={nextDisplayOrder}
          isSaving={createMutation.isPending || updateMutation.isPending}
          isError={createMutation.isError || updateMutation.isError}
          isSuccess={createMutation.isSuccess || updateMutation.isSuccess}
          onSubmit={submitService}
          onCancel={() => setFormMode({ type: "closed" })}
        />
      ) : null}

      {services.length === 0 ? (
        <EmptyServices onCreate={() => setFormMode({ type: "create" })} />
      ) : (
        <ServiceList
          services={services}
          isBusy={isMutating}
          onEdit={(service) => setFormMode({ type: "edit", service })}
          onToggleStatus={(service) => void toggleStatus(service)}
          onMoveUp={(service) => void moveService(service, "up")}
          onMoveDown={(service) => void moveService(service, "down")}
          onDelete={setServiceToDelete}
        />
      )}

      {serviceToDelete ? (
        <DeleteDialog
          service={serviceToDelete}
          isDeleting={deleteMutation.isPending}
          onCancel={() => setServiceToDelete(null)}
          onConfirm={() => void confirmDelete()}
        />
      ) : null}
    </div>
  );
}
