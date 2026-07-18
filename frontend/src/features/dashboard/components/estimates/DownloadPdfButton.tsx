"use client";

import { Download, FileWarning, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { useDownloadEstimatePdf } from "../../hooks/estimate-hooks";

type DownloadPdfButtonProps = {
  estimateId: string;
  /** Used only as a filename fallback if the server response has no Content-Disposition. */
  estimateNumber: string;
};

/**
 * Downloads the backend-generated PDF and saves it via a transient object URL — never
 * renders or previews the PDF in the browser, and never recomputes anything the file
 * already contains.
 */
export function DownloadPdfButton({ estimateId, estimateNumber }: DownloadPdfButtonProps) {
  const downloadMutation = useDownloadEstimatePdf();

  async function handleClick() {
    if (downloadMutation.isPending) {
      return; // Prevent duplicate clicks while a download is already in flight.
    }

    const { blob, filename } = await downloadMutation.mutateAsync(estimateId);
    const objectUrl = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = filename ?? `orcamento-${estimateNumber}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
  }

  return (
    <div className="space-y-2">
      <Button
        type="button"
        variant="secondary"
        onClick={() => void handleClick()}
        disabled={downloadMutation.isPending}
        aria-busy={downloadMutation.isPending}
      >
        {downloadMutation.isPending ? (
          <Loader2 size={16} className="animate-spin" aria-hidden="true" />
        ) : (
          <Download size={16} aria-hidden="true" />
        )}
        {downloadMutation.isPending ? "A gerar PDF" : "Baixar PDF"}
      </Button>
      {downloadMutation.isError ? (
        <p className="m-0 flex items-center gap-2 text-sm font-semibold text-error">
          <FileWarning size={16} aria-hidden="true" />
          Não foi possível gerar o PDF. Tente novamente.
        </p>
      ) : null}
    </div>
  );
}
