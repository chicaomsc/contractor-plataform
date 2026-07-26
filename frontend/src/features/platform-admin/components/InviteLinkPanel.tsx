"use client";

import { Copy } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { buildInviteLink, copyToClipboard } from "../utils/invite-links";

export function InviteLinkPanel({
  token,
  expiresAt,
}: {
  token: string;
  expiresAt: string;
}) {
  const [copied, setCopied] = useState(false);
  const link = buildInviteLink(token);

  async function copyLink() {
    await copyToClipboard(link);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  return (
    <div className="border border-border bg-background p-4">
      <p className="m-0 text-sm font-semibold">Link de convite</p>
      <div className="mt-3 flex flex-col gap-3 md:flex-row">
        <input
          readOnly
          value={link}
          className="min-h-12 min-w-0 flex-1 border border-border bg-surface px-3 text-sm"
          aria-label="Link de convite"
        />
        <Button type="button" onClick={copyLink}>
          <Copy size={16} aria-hidden="true" />
          {copied ? "Copiado" : "Copiar link de convite"}
        </Button>
      </div>
      <p className="mb-0 mt-2 text-xs text-[var(--muted-foreground)]">
        Expira em {new Intl.DateTimeFormat("pt-PT", {
          dateStyle: "medium",
          timeStyle: "short",
        }).format(new Date(expiresAt))}
      </p>
    </div>
  );
}
