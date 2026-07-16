import type { GalleryDto } from "../../types/admin";
import { GalleryCard } from "./GalleryCard";

type GalleryGridProps = {
  items: GalleryDto[];
  isBusy: boolean;
  isUploading: boolean;
  onEdit: (item: GalleryDto) => void;
  onPreview: (item: GalleryDto) => void;
  onToggleStatus: (item: GalleryDto) => void;
  onToggleFeatured: (item: GalleryDto) => void;
  onMoveUp: (item: GalleryDto) => void;
  onMoveDown: (item: GalleryDto) => void;
  onUpload: (
    item: GalleryDto,
    slot: "before" | "after",
    file: File,
  ) => Promise<void>;
  onDeleteSlot: (item: GalleryDto, slot: "before" | "after") => void;
  onDelete: (item: GalleryDto) => void;
};

export function sortGalleryItems(items: GalleryDto[]) {
  return [...items].sort((a, b) => {
    if (a.displayOrder !== b.displayOrder) {
      return a.displayOrder - b.displayOrder;
    }

    return a.title.localeCompare(b.title, "pt");
  });
}

export function GalleryGrid({
  items,
  isBusy,
  isUploading,
  onEdit,
  onPreview,
  onToggleStatus,
  onToggleFeatured,
  onMoveUp,
  onMoveDown,
  onUpload,
  onDeleteSlot,
  onDelete,
}: GalleryGridProps) {
  const sortedItems = sortGalleryItems(items);

  return (
    <div className="grid gap-4 xl:grid-cols-2">
      {sortedItems.map((item, index) => (
        <GalleryCard
          key={item.id}
          item={item}
          isFirst={index === 0}
          isLast={index === sortedItems.length - 1}
          isBusy={isBusy}
          isUploading={isUploading}
          onEdit={onEdit}
          onPreview={onPreview}
          onToggleStatus={onToggleStatus}
          onToggleFeatured={onToggleFeatured}
          onMoveUp={onMoveUp}
          onMoveDown={onMoveDown}
          onUpload={onUpload}
          onDeleteSlot={onDeleteSlot}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
}
