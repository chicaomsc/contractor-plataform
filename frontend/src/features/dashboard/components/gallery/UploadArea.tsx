"use client";

import { Loader2, Upload } from "lucide-react";
import { useRef, useState } from "react";
import { Button } from "@/components/ui/Button";

const MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024;
const ALLOWED_UPLOAD_TYPES = new Map([
  ["image/png", [".png"]],
  ["image/jpeg", [".jpg", ".jpeg"]],
  ["image/webp", [".webp"]],
]);

export function validateGalleryImageFile(file: File) {
  const allowedExtensions = ALLOWED_UPLOAD_TYPES.get(file.type);
  const fileName = file.name.toLowerCase();

  if (
    !allowedExtensions ||
    !allowedExtensions.some((extension) => fileName.endsWith(extension))
  ) {
    return "Formato inválido. Envie uma imagem PNG, JPEG ou WebP.";
  }

  if (file.size > MAX_UPLOAD_SIZE_BYTES) {
    return "Arquivo inválido. O tamanho máximo é 5 MB.";
  }

  return null;
}

type UploadAreaProps = {
  slot: "before" | "after";
  label: string;
  isUploading: boolean;
  onUpload: (slot: "before" | "after", file: File) => Promise<void>;
};

export function UploadArea({
  slot,
  label,
  isUploading,
  onUpload,
}: UploadAreaProps) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  async function handleFile(file: File | null) {
    if (!file) {
      return;
    }

    const validationError = validateGalleryImageFile(file);
    if (validationError) {
      setError(validationError);
      setProgress(0);
      return;
    }

    setError(null);
    setProgress(20);

    try {
      setProgress(65);
      await onUpload(slot, file);
      setProgress(100);
      window.setTimeout(() => setProgress(0), 1200);
    } catch {
      setProgress(0);
      setError("Não foi possível enviar a imagem.");
    } finally {
      if (inputRef.current) {
        inputRef.current.value = "";
      }
    }
  }

  return (
    <div className="border border-border bg-background p-3">
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp"
        className="sr-only"
        onChange={(event) => void handleFile(event.target.files?.[0] ?? null)}
      />
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="w-full"
        disabled={isUploading}
        onClick={() => inputRef.current?.click()}
      >
        {isUploading ? (
          <Loader2 size={16} className="animate-spin" aria-hidden="true" />
        ) : (
          <Upload size={16} aria-hidden="true" />
        )}
        {isUploading ? "A enviar" : label}
      </Button>
      {progress > 0 ? (
        <div className="mt-3" aria-label={`Progresso de upload ${label}`}>
          <div className="h-2 bg-surface-muted">
            <div
              className="h-full bg-primary transition-[width]"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="m-0 mt-1 text-xs font-semibold text-[var(--muted-foreground)]">
            {progress}%
          </p>
        </div>
      ) : null}
      {error ? (
        <p className="m-0 mt-2 text-xs font-semibold text-error">{error}</p>
      ) : null}
    </div>
  );
}
