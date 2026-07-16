import { X } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { GalleryDto } from "../../types/admin";
import { FeaturedBadge } from "./FeaturedBadge";
import { ImagePreview } from "./ImagePreview";
import { ImageStatusBadge } from "./ImageStatusBadge";

type GalleryPreviewDialogProps = {
  item: GalleryDto;
  onClose: () => void;
};

export function GalleryPreviewDialog({
  item,
  onClose,
}: GalleryPreviewDialogProps) {
  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/45 px-4"
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="gallery-preview-title"
        className="max-h-[90vh] w-full max-w-5xl overflow-y-auto border border-border bg-surface p-6 shadow-sm"
      >
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <ImageStatusBadge active={item.active} />
              <FeaturedBadge featured={item.featured} />
            </div>
            <h2
              id="gallery-preview-title"
              className="m-0 mt-4 font-display text-3xl font-semibold"
            >
              {item.title}
            </h2>
            <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
              {item.description ?? "Sem descrição configurada."}
            </p>
          </div>
          <Button type="button" variant="ghost" size="sm" onClick={onClose}>
            <X size={16} aria-hidden="true" />
            Fechar
          </Button>
        </div>

        <div className="mt-6 grid gap-5 md:grid-cols-2">
          <ImagePreview
            src={item.beforeImageUrl}
            label="Before"
            title={item.title}
          />
          <ImagePreview
            src={item.afterImageUrl}
            label="After"
            title={item.title}
          />
        </div>
      </div>
    </div>
  );
}
