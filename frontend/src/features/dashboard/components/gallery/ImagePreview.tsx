"use client";

import Image from "next/image";
import { ImageOff } from "lucide-react";
import { resolveAdminAssetUrl } from "../../utils/assets";

type ImagePreviewProps = {
  src: string | null;
  label: string;
  title: string;
};

export function ImagePreview({ src, label, title }: ImagePreviewProps) {
  const resolvedSrc = resolveAdminAssetUrl(src);

  return (
    <figure className="m-0 min-w-0">
      <div className="relative flex aspect-[4/3] items-center justify-center overflow-hidden border border-border bg-background">
        {resolvedSrc ? (
          <Image
            src={resolvedSrc}
            alt={`${label}: ${title}`}
            fill
            sizes="(min-width: 1024px) 360px, 100vw"
            className="object-cover"
          />
        ) : (
          <div className="flex flex-col items-center gap-2 text-sm font-semibold text-[var(--muted-foreground)]">
            <ImageOff size={22} aria-hidden="true" />
            Sem imagem
          </div>
        )}
      </div>
      <figcaption className="mt-2 text-xs font-bold uppercase tracking-[0.12em] text-[var(--muted-foreground)]">
        {label}
      </figcaption>
    </figure>
  );
}
