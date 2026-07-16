import { Images, Plus } from "lucide-react";
import { Button } from "@/components/ui/Button";

type EmptyGalleryProps = {
  onCreate: () => void;
};

export function EmptyGallery({ onCreate }: EmptyGalleryProps) {
  return (
    <div className="border border-dashed border-border bg-surface p-8 text-center">
      <Images
        size={34}
        className="mx-auto text-[var(--muted-foreground)]"
        aria-hidden="true"
      />
      <h2 className="m-0 mt-4 font-display text-2xl font-semibold">
        Sem imagens cadastradas
      </h2>
      <p className="mx-auto mt-2 max-w-xl text-sm text-[var(--muted-foreground)]">
        Crie um item de galeria e envie imagens before/after usando os endpoints
        existentes.
      </p>
      <div className="mt-6">
        <Button type="button" onClick={onCreate}>
          <Plus size={16} aria-hidden="true" />
          Criar item
        </Button>
      </div>
    </div>
  );
}
