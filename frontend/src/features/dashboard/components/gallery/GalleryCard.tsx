import {
  ArrowDown,
  ArrowUp,
  Eye,
  Pencil,
  Power,
  PowerOff,
  Star,
  StarOff,
  Trash2,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { GalleryDto } from "../../types/admin";
import { FeaturedBadge } from "./FeaturedBadge";
import { ImagePreview } from "./ImagePreview";
import { ImageStatusBadge } from "./ImageStatusBadge";
import { UploadArea } from "./UploadArea";

type GalleryCardProps = {
  item: GalleryDto;
  isFirst: boolean;
  isLast: boolean;
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

export function GalleryCard({
  item,
  isFirst,
  isLast,
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
}: GalleryCardProps) {
  const hasBefore = Boolean(item.beforeImageUrl);
  const hasAfter = Boolean(item.afterImageUrl);

  return (
    <article className="border border-border bg-surface p-5">
      <div className="flex flex-wrap items-center gap-3">
        <span className="font-display text-sm font-bold text-primary">
          {item.displayOrder.toString().padStart(2, "0")}
        </span>
        <ImageStatusBadge active={item.active} />
        <FeaturedBadge featured={item.featured} />
        <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--muted-foreground)]">
          {hasBefore && hasAfter ? "Before/After" : "Imagem parcial"}
        </span>
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <ImagePreview
          src={item.beforeImageUrl}
          label="Before"
          title={item.title}
        />
        <ImagePreview src={item.afterImageUrl} label="After" title={item.title} />
      </div>

      <div className="mt-5 min-w-0">
        <h2 className="m-0 font-display text-2xl font-semibold leading-tight">
          {item.title}
        </h2>
        <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
          {item.description ?? "Sem descrição configurada."}
        </p>
      </div>

      <div className="mt-5 grid gap-3 md:grid-cols-2">
        <UploadArea
          slot="before"
          label={hasBefore ? "Substituir before" : "Enviar before"}
          isUploading={isUploading}
          onUpload={(slot, file) => onUpload(item, slot, file)}
        />
        <UploadArea
          slot="after"
          label={hasAfter ? "Substituir after" : "Enviar after"}
          isUploading={isUploading}
          onUpload={(slot, file) => onUpload(item, slot, file)}
        />
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onMoveUp(item)}
          disabled={isFirst || isBusy}
          aria-label={`Mover ${item.title} para cima`}
        >
          <ArrowUp size={16} aria-hidden="true" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onMoveDown(item)}
          disabled={isLast || isBusy}
          aria-label={`Mover ${item.title} para baixo`}
        >
          <ArrowDown size={16} aria-hidden="true" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onPreview(item)}
        >
          <Eye size={16} aria-hidden="true" />
          Preview
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onToggleFeatured(item)}
          disabled={isBusy}
        >
          {item.featured ? (
            <StarOff size={16} aria-hidden="true" />
          ) : (
            <Star size={16} aria-hidden="true" />
          )}
          {item.featured ? "Remover destaque" : "Destacar"}
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onToggleStatus(item)}
          disabled={isBusy}
        >
          {item.active ? (
            <PowerOff size={16} aria-hidden="true" />
          ) : (
            <Power size={16} aria-hidden="true" />
          )}
          {item.active ? "Desativar" : "Ativar"}
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onEdit(item)}
          disabled={isBusy}
        >
          <Pencil size={16} aria-hidden="true" />
          Editar
        </Button>
        {hasBefore ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onDeleteSlot(item, "before")}
            disabled={isBusy}
          >
            <X size={16} aria-hidden="true" />
            Before
          </Button>
        ) : null}
        {hasAfter ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onDeleteSlot(item, "after")}
            disabled={isBusy}
          >
            <X size={16} aria-hidden="true" />
            After
          </Button>
        ) : null}
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => onDelete(item)}
          disabled={isBusy}
        >
          <Trash2 size={16} aria-hidden="true" />
          Excluir
        </Button>
      </div>
    </article>
  );
}
