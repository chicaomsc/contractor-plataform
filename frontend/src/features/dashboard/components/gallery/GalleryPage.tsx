"use client";

import { Plus } from "lucide-react";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import {
  useCreateGalleryItem,
  useDeleteGalleryImage,
  useDeleteGalleryItem,
  useFeatureGalleryItem,
  useGallery,
  useReorderGalleryItem,
  useUpdateGalleryItem,
  useUploadGalleryImage,
} from "../../hooks/dashboard-hooks";
import type { GalleryDto, GalleryFormInput } from "../../types/admin";
import { ErrorState, LoadingState, SaveFeedback } from "../DashboardState";
import { PageHeader } from "../PageHeader";
import { DeleteImageDialog } from "./DeleteImageDialog";
import { EmptyGallery } from "./EmptyGallery";
import { GalleryForm } from "./GalleryForm";
import { GalleryGrid, sortGalleryItems } from "./GalleryGrid";
import { GalleryPreviewDialog } from "./GalleryPreviewDialog";

type FormMode =
  | { type: "closed" }
  | { type: "create" }
  | { type: "edit"; item: GalleryDto };

function toGalleryPayload(item: GalleryDto): GalleryFormInput {
  return {
    title: item.title,
    description: item.description ?? "",
    displayOrder: item.displayOrder,
    featured: item.featured,
    active: item.active,
  };
}

export function GalleryPage() {
  const galleryQuery = useGallery();
  const createMutation = useCreateGalleryItem();
  const updateMutation = useUpdateGalleryItem();
  const deleteMutation = useDeleteGalleryItem();
  const featureMutation = useFeatureGalleryItem();
  const reorderMutation = useReorderGalleryItem();
  const uploadMutation = useUploadGalleryImage();
  const deleteImageMutation = useDeleteGalleryImage();
  const [formMode, setFormMode] = useState<FormMode>({ type: "closed" });
  const [itemToDelete, setItemToDelete] = useState<GalleryDto | null>(null);
  const [previewItem, setPreviewItem] = useState<GalleryDto | null>(null);

  const items = useMemo(
    () => sortGalleryItems(galleryQuery.data ?? []),
    [galleryQuery.data],
  );
  const nextDisplayOrder =
    items.length > 0
      ? Math.max(...items.map((item) => item.displayOrder)) + 1
      : 0;
  const isMutating =
    createMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending ||
    featureMutation.isPending ||
    reorderMutation.isPending ||
    uploadMutation.isPending ||
    deleteImageMutation.isPending;

  async function submitItem(values: GalleryFormInput) {
    if (formMode.type === "edit") {
      await updateMutation.mutateAsync({
        galleryItemId: formMode.item.id,
        payload: values,
      });
      setFormMode({ type: "closed" });
      return;
    }

    await createMutation.mutateAsync(values);
    setFormMode({ type: "closed" });
  }

  async function toggleStatus(item: GalleryDto) {
    await updateMutation.mutateAsync({
      galleryItemId: item.id,
      payload: {
        ...toGalleryPayload(item),
        active: !item.active,
      },
    });
  }

  async function toggleFeatured(item: GalleryDto) {
    await featureMutation.mutateAsync({
      galleryItemId: item.id,
      featured: !item.featured,
    });
  }

  async function moveItem(item: GalleryDto, direction: "up" | "down") {
    const currentIndex = items.findIndex((current) => current.id === item.id);
    const targetIndex = direction === "up" ? currentIndex - 1 : currentIndex + 1;
    const target = items[targetIndex];

    if (!target) {
      return;
    }

    await Promise.all([
      reorderMutation.mutateAsync({
        galleryItemId: item.id,
        displayOrder: target.displayOrder,
      }),
      reorderMutation.mutateAsync({
        galleryItemId: target.id,
        displayOrder: item.displayOrder,
      }),
    ]);
  }

  async function uploadImage(
    item: GalleryDto,
    slot: "before" | "after",
    file: File,
  ) {
    await uploadMutation.mutateAsync({
      galleryItemId: item.id,
      slot,
      file,
    });
  }

  async function deleteImage(item: GalleryDto, slot: "before" | "after") {
    await deleteImageMutation.mutateAsync({
      galleryItemId: item.id,
      slot,
    });
  }

  async function confirmDelete() {
    if (!itemToDelete) {
      return;
    }

    await deleteMutation.mutateAsync(itemToDelete.id);
    setItemToDelete(null);
  }

  if (galleryQuery.isLoading) {
    return <LoadingState label="A carregar galeria" />;
  }

  if (galleryQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar a galeria"
        description="A listagem consome o endpoint autenticado /gallery."
        onRetry={() => void galleryQuery.refetch()}
      />
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Galeria"
        title="Gerenciar galeria"
        description="Administre imagens, pares before/after, destaque e publicação na landing."
        action={
          <Button type="button" onClick={() => setFormMode({ type: "create" })}>
            <Plus size={16} aria-hidden="true" />
            Novo item
          </Button>
        }
      />

      <SaveFeedback
        isError={
          createMutation.isError ||
          updateMutation.isError ||
          deleteMutation.isError ||
          featureMutation.isError ||
          reorderMutation.isError ||
          uploadMutation.isError ||
          deleteImageMutation.isError
        }
        isSuccess={
          createMutation.isSuccess ||
          updateMutation.isSuccess ||
          deleteMutation.isSuccess ||
          featureMutation.isSuccess ||
          reorderMutation.isSuccess ||
          uploadMutation.isSuccess ||
          deleteImageMutation.isSuccess
        }
      />

      {formMode.type !== "closed" ? (
        <GalleryForm
          item={formMode.type === "edit" ? formMode.item : null}
          nextDisplayOrder={nextDisplayOrder}
          isSaving={createMutation.isPending || updateMutation.isPending}
          isError={createMutation.isError || updateMutation.isError}
          isSuccess={createMutation.isSuccess || updateMutation.isSuccess}
          onSubmit={submitItem}
          onCancel={() => setFormMode({ type: "closed" })}
        />
      ) : null}

      {items.length === 0 ? (
        <EmptyGallery onCreate={() => setFormMode({ type: "create" })} />
      ) : (
        <GalleryGrid
          items={items}
          isBusy={isMutating}
          isUploading={uploadMutation.isPending}
          onEdit={(item) => setFormMode({ type: "edit", item })}
          onPreview={setPreviewItem}
          onToggleStatus={(item) => void toggleStatus(item)}
          onToggleFeatured={(item) => void toggleFeatured(item)}
          onMoveUp={(item) => void moveItem(item, "up")}
          onMoveDown={(item) => void moveItem(item, "down")}
          onUpload={uploadImage}
          onDeleteSlot={(item, slot) => void deleteImage(item, slot)}
          onDelete={setItemToDelete}
        />
      )}

      {previewItem ? (
        <GalleryPreviewDialog
          item={previewItem}
          onClose={() => setPreviewItem(null)}
        />
      ) : null}

      {itemToDelete ? (
        <DeleteImageDialog
          item={itemToDelete}
          isDeleting={deleteMutation.isPending}
          onCancel={() => setItemToDelete(null)}
          onConfirm={() => void confirmDelete()}
        />
      ) : null}
    </div>
  );
}
