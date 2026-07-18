"use client";

import { Check, Copy, Link2, MessageCircle, Share2, ShieldOff } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import {
  useCreateEstimateShare,
  useEstimateShare,
  useRevokeEstimateShare,
} from "../../hooks/estimate-share-hooks";
import { buildEstimateShareUrl, buildEstimateShareWhatsAppHref } from "../../utils/estimate-share-links";
import { formatDate } from "../../utils/money";

type ShareEstimatePanelProps = {
  estimateId: string;
};

/**
 * Only the response from a create/regenerate call ever contains the raw token — the
 * backend persists just its hash, so a later page load can show the link's status
 * (active/expired/revoked, access count) but never the link itself again. Regenerating
 * is the recovery path, and it revokes the previous link (see EstimateShareService).
 */
export function ShareEstimatePanel({ estimateId }: ShareEstimatePanelProps) {
  const shareQuery = useEstimateShare(estimateId);
  const createMutation = useCreateEstimateShare(estimateId);
  const revokeMutation = useRevokeEstimateShare(estimateId);
  const [revealedToken, setRevealedToken] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const hasNoShare = shareQuery.isError && shareQuery.error instanceof ApiError && shareQuery.error.status === 404;
  const share = shareQuery.data;
  const isActive = share?.status === "ACTIVE";

  async function handleCreate() {
    setCopied(false);
    const result = await createMutation.mutateAsync({});
    setRevealedToken(result.token);
  }

  async function handleRevoke() {
    await revokeMutation.mutateAsync();
    setRevealedToken(null);
    setCopied(false);
  }

  async function handleCopy(shareUrl: string) {
    await navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (shareQuery.isLoading) {
    return (
      <p className="m-0 text-sm text-[var(--muted-foreground)]">A verificar link de partilha…</p>
    );
  }

  if (shareQuery.isError && !hasNoShare) {
    return (
      <p className="m-0 text-sm font-semibold text-error">
        Não foi possível carregar o estado da partilha.
      </p>
    );
  }

  if (isActive) {
    const shareUrl = revealedToken ? buildEstimateShareUrl(revealedToken) : null;

    return (
      <div className="space-y-3 border border-border bg-surface p-4">
        <div className="flex items-center gap-2">
          <Link2 size={16} className="text-primary" aria-hidden="true" />
          <p className="m-0 text-sm font-semibold">Link de partilha ativo</p>
        </div>

        {shareUrl ? (
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              readOnly
              value={shareUrl}
              onFocus={(event) => event.currentTarget.select()}
              className="min-w-0 flex-1 border border-border bg-background px-3 py-2 text-sm"
              aria-label="Link público do orçamento"
            />
            <div className="flex gap-2">
              <Button type="button" variant="secondary" size="sm" onClick={() => void handleCopy(shareUrl)}>
                {copied ? <Check size={16} aria-hidden="true" /> : <Copy size={16} aria-hidden="true" />}
                {copied ? "Copiado" : "Copiar link"}
              </Button>
              <a
                href={buildEstimateShareWhatsAppHref(shareUrl)}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex min-h-12 items-center justify-center gap-2 border border-foreground bg-transparent px-4 py-2 text-sm font-semibold text-foreground no-underline transition-colors hover:bg-foreground hover:text-surface"
              >
                <MessageCircle size={16} aria-hidden="true" />
                WhatsApp
              </a>
            </div>
          </div>
        ) : (
          <p className="m-0 text-sm text-[var(--muted-foreground)]">
            Por segurança, o link só é mostrado no momento em que é criado. Gere um novo
            link para o copiar novamente.
          </p>
        )}

        <p className="m-0 text-xs text-[var(--muted-foreground)]">
          Expira em {formatDate(share.expiresAt)} · Acedido {share.accessCount}{" "}
          {share.accessCount === 1 ? "vez" : "vezes"}
          {share.lastAccessAt ? ` · último acesso em ${formatDate(share.lastAccessAt)}` : ""}
        </p>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => void handleCreate()}
            disabled={createMutation.isPending}
          >
            <Share2 size={16} aria-hidden="true" />
            {createMutation.isPending ? "A gerar" : "Gerar novo link"}
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => void handleRevoke()}
            disabled={revokeMutation.isPending}
          >
            <ShieldOff size={16} aria-hidden="true" />
            {revokeMutation.isPending ? "A revogar" : "Revogar"}
          </Button>
        </div>
        {revokeMutation.isError ? (
          <p className="m-0 text-sm font-semibold text-error">Não foi possível revogar o link.</p>
        ) : null}
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <Button type="button" variant="secondary" onClick={() => void handleCreate()} disabled={createMutation.isPending}>
        <Share2 size={16} aria-hidden="true" />
        {createMutation.isPending ? "A gerar link" : "Compartilhar"}
      </Button>
      {createMutation.isError ? (
        <p className="m-0 text-sm font-semibold text-error">Não foi possível criar o link de partilha.</p>
      ) : null}
    </div>
  );
}
