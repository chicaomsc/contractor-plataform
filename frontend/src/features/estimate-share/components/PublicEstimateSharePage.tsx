"use client";

import { Download } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import { buildPublicEstimateSharePdfUrl } from "../api/estimate-share-api";
import { usePublicEstimateShare } from "../hooks/estimate-share-hooks";
import type { PublicEstimateShareLineItemDto } from "../types/api";
import { ShareLoadingState, ShareUnavailableState } from "./PublicEstimateShareStates";

function LineItemsTable({ title, items }: { title: string; items: PublicEstimateShareLineItemDto[] }) {
  if (items.length === 0) {
    return null;
  }

  return (
    <div className="space-y-3">
      <h2 className="m-0 font-display text-lg font-semibold">{title}</h2>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[480px] border-collapse text-sm">
          <thead>
            <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-[var(--muted-foreground)]">
              <th className="py-2 pr-4 font-semibold">Descrição</th>
              <th className="py-2 pr-4 font-semibold">Qtd.</th>
              <th className="py-2 pr-4 font-semibold">Preço unit.</th>
              <th className="py-2 text-right font-semibold">Total</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={`${item.description}-${index}`} className="border-b border-border/60">
                <td className="py-2 pr-4">{item.description}</td>
                <td className="py-2 pr-4">
                  {item.quantity} {item.unit}
                </td>
                <td className="py-2 pr-4">{item.unitPrice}</td>
                <td className="py-2 text-right font-semibold">{item.total}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function PublicEstimateSharePage({ token }: { token: string }) {
  const shareQuery = usePublicEstimateShare(token);

  if (shareQuery.isLoading) {
    return <ShareLoadingState />;
  }

  if (shareQuery.isError || !shareQuery.data) {
    return <ShareUnavailableState />;
  }

  const { seller, estimate, customer, items, materials, summary, notes, terms } = shareQuery.data;
  const pdfUrl = buildPublicEstimateSharePdfUrl(token);

  return (
    <Section labelledBy="share-title">
      <Container size="narrow" className="space-y-10">
        <header className="flex flex-col gap-6 border-b border-border pb-8 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-start gap-4">
            {seller.logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={seller.logoUrl}
                alt={seller.displayName ?? "Logótipo"}
                className="h-14 w-14 object-contain"
              />
            ) : null}
            <div>
              <p className="m-0 text-xs font-bold uppercase tracking-[0.18em] text-primary">
                {estimate.number}
              </p>
              <h1 id="share-title" className="m-0 mt-1 font-display text-2xl font-bold sm:text-3xl">
                {estimate.title}
              </h1>
              {seller.displayName ? (
                <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">{seller.displayName}</p>
              ) : null}
            </div>
          </div>
          <div className="flex flex-col items-start gap-3 sm:items-end">
            <span className="inline-block border border-foreground px-3 py-1 text-xs font-bold uppercase tracking-wide">
              {estimate.status}
            </span>
            <a
              href={pdfUrl}
              className="inline-flex min-h-12 items-center justify-center gap-2 border border-primary bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground no-underline transition-colors hover:bg-primary-hover"
            >
              <Download size={16} aria-hidden="true" />
              Baixar PDF
            </a>
          </div>
        </header>

        {estimate.draft ? (
          <p className="m-0 border-l-[3px] border-warning bg-surface-muted px-4 py-3 text-sm">
            Este orçamento ainda está em rascunho — os valores podem ser alterados pelo prestador.
          </p>
        ) : null}
        {estimate.cancelled ? (
          <p className="m-0 border-l-[3px] border-error bg-surface-muted px-4 py-3 text-sm">
            Este orçamento foi cancelado.
          </p>
        ) : null}

        <div className="grid gap-6 sm:grid-cols-2">
          <div>
            <h2 className="m-0 font-display text-lg font-semibold">Cliente</h2>
            <p className="m-0 mt-2 text-sm">{customer.name ?? "—"}</p>
          </div>
          <div>
            <h2 className="m-0 font-display text-lg font-semibold">Detalhes</h2>
            <dl className="m-0 mt-2 space-y-1 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-[var(--muted-foreground)]">Emitido em</dt>
                <dd className="m-0">{estimate.issueDate ?? "—"}</dd>
              </div>
              {estimate.validUntil ? (
                <div className="flex justify-between gap-4">
                  <dt className="text-[var(--muted-foreground)]">Válido até</dt>
                  <dd className="m-0">{estimate.validUntil}</dd>
                </div>
              ) : null}
            </dl>
          </div>
        </div>

        {estimate.description ? <p className="m-0 text-sm">{estimate.description}</p> : null}

        <LineItemsTable title="Serviços" items={items} />
        <LineItemsTable title="Materiais" items={materials} />

        <div className="ml-auto max-w-sm space-y-2 border-t border-border pt-4 text-sm">
          <div className="flex justify-between">
            <span>Mão de obra</span>
            <span>{summary.laborSubtotal}</span>
          </div>
          <div className="flex justify-between">
            <span>Materiais</span>
            <span>{summary.materialSubtotal}</span>
          </div>
          <div className="flex justify-between">
            <span>Subtotal</span>
            <span>{summary.subtotal}</span>
          </div>
          <div className="flex justify-between">
            <span>{summary.vatLabel}</span>
            <span>{summary.vatAmount}</span>
          </div>
          <div className="flex justify-between text-base font-bold">
            <span>Total</span>
            <span>{summary.total}</span>
          </div>
          <div className="flex justify-between">
            <span>{summary.upfrontLabel}</span>
            <span>{summary.upfrontAmount}</span>
          </div>
          <div className="flex justify-between">
            <span>Restante</span>
            <span>{summary.remaining}</span>
          </div>
        </div>

        {notes ? (
          <div>
            <h2 className="m-0 font-display text-lg font-semibold">Observações</h2>
            <p className="m-0 mt-2 whitespace-pre-wrap text-sm">{notes}</p>
          </div>
        ) : null}
        {terms ? (
          <div>
            <h2 className="m-0 font-display text-lg font-semibold">Condições</h2>
            <p className="m-0 mt-2 whitespace-pre-wrap text-sm">{terms}</p>
          </div>
        ) : null}

        {seller.phone || seller.email || seller.website || seller.addressLine ? (
          <footer className="border-t border-border pt-6 text-sm text-[var(--muted-foreground)]">
            {seller.displayName ? (
              <p className="m-0 font-semibold text-foreground">{seller.displayName}</p>
            ) : null}
            {seller.addressLine ? <p className="m-0">{seller.addressLine}</p> : null}
            {seller.phone ? <p className="m-0">{seller.phone}</p> : null}
            {seller.email ? <p className="m-0">{seller.email}</p> : null}
            {seller.website ? <p className="m-0">{seller.website}</p> : null}
          </footer>
        ) : null}
      </Container>
    </Section>
  );
}
