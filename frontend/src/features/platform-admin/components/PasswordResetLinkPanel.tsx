"use client";

import { Copy, X } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { copyToClipboard } from "../utils/invite-links";

export function PasswordResetLinkPanel({
  resetLink,
  expiresAt,
  onClose,
}: {
  resetLink: string;
  expiresAt: string;
  onClose: () => void;
}) {
  const [copied, setCopied] = useState(false);

  async function copyLink() {
    await copyToClipboard(resetLink);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  return (
    <div className="border border-border bg-background p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="m-0 text-sm font-semibold">Link de recuperação</p>
          <p className="m-0 mt-1 text-xs text-[var(--muted-foreground)]">
            Mostrado uma única vez. Entregue este link manualmente ao owner.
          </p>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          aria-label="Fechar link de recuperação"
          onClick={onClose}
        >
          <X size={16} aria-hidden="true" />
        </Button>
      </div>
      <div className="mt-3 flex flex-col gap-3 md:flex-row">
        <input
          readOnly
          value={resetLink}
          className="min-h-12 min-w-0 flex-1 border border-border bg-surface px-3 text-sm"
          aria-label="Link de recuperação"
        />
        <Button type="button" onClick={copyLink}>
          <Copy size={16} aria-hidden="true" />
          {copied ? "Copiado" : "Copiar link"}
        </Button>
      </div>
      <p className="mb-0 mt-2 text-xs text-[var(--muted-foreground)]">
        Expira em{" "}
        {new Intl.DateTimeFormat("pt-PT", {
          dateStyle: "medium",
          timeStyle: "short",
        }).format(new Date(expiresAt))}
      </p>
    </div>
  );
}
