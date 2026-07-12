"use client";

import type { UseQueryResult } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/errors";
import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import type {
  PublicGalleryItemViewModel,
  PublicServiceViewModel,
  PublicSiteViewModel,
} from "../types/view-model";

type PreviewProps = {
  companySlug: string | null;
  siteQuery: UseQueryResult<PublicSiteViewModel, Error>;
  servicesQuery: UseQueryResult<PublicServiceViewModel[], Error>;
  galleryQuery: UseQueryResult<PublicGalleryItemViewModel[], Error>;
};

function getErrorMessage(error: Error | null, fallback: string) {
  if (error instanceof ApiError && error.status === 404) {
    return "Não encontrámos este site público. Verifique o slug configurado.";
  }

  if (error instanceof ApiError) {
    return error.message;
  }

  return fallback;
}

function SkeletonLine({ width = "w-full" }: { width?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`block h-4 ${width} animate-pulse bg-surface-muted`}
    />
  );
}

function LoadingBlock({ label }: { label: string }) {
  return (
    <div aria-busy="true" aria-label={label} className="space-y-3">
      <SkeletonLine width="w-40" />
      <SkeletonLine />
      <SkeletonLine width="w-2/3" />
    </div>
  );
}

function StatusBadge({ children }: { children: string }) {
  return (
    <span className="inline-flex border border-border bg-surface px-2 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-[var(--muted-foreground)]">
      {children}
    </span>
  );
}

function PartialError({ children }: { children: string }) {
  return (
    <p
      role="status"
      className="border-l-4 border-warning bg-surface px-4 py-3 text-sm"
    >
      {children}
    </p>
  );
}

export function PublicSiteIntegrationPreview({
  companySlug,
  siteQuery,
  servicesQuery,
  galleryQuery,
}: PreviewProps) {
  const services = servicesQuery.data ?? [];
  const gallery = galleryQuery.data ?? [];
  const completePairs = gallery.filter(
    (item) => item.hasCompleteBeforeAfterPair,
  );

  if (!companySlug) {
    return (
      <Section labelledBy="integration-error-title" className="min-h-[48vh]">
        <Container size="narrow">
          <div
            role="alert"
            tabIndex={-1}
            className="border-l-4 border-error bg-surface px-5 py-4"
          >
            <h1
              id="integration-error-title"
              className="m-0 font-display text-[var(--text-display-md)] font-semibold"
            >
              Slug público não configurado
            </h1>
            <p className="mb-0 text-[var(--muted-foreground)]">
              Configure `NEXT_PUBLIC_COMPANY_SLUG` para carregar os dados
              públicos.
            </p>
          </div>
        </Container>
      </Section>
    );
  }

  if (siteQuery.isLoading) {
    return (
      <Section labelledBy="integration-loading-title" className="min-h-[48vh]">
        <Container size="narrow">
          <h1
            id="integration-loading-title"
            className="font-display text-[var(--text-display-md)] font-semibold"
          >
            A carregar site público
          </h1>
          <LoadingBlock label="A carregar dados do site público" />
        </Container>
      </Section>
    );
  }

  if (siteQuery.isError || !siteQuery.data) {
    return (
      <Section labelledBy="integration-error-title" className="min-h-[48vh]">
        <Container size="narrow">
          <div
            role="alert"
            tabIndex={-1}
            className="border-l-4 border-error bg-surface px-5 py-4"
          >
            <StatusBadge>Erro bloqueante</StatusBadge>
            <h1
              id="integration-error-title"
              className="mt-4 font-display text-[var(--text-display-md)] font-semibold"
            >
              Site público indisponível
            </h1>
            <p className="mb-0 text-[var(--muted-foreground)]">
              {getErrorMessage(
                siteQuery.error,
                "Não foi possível carregar os dados públicos do site.",
              )}
            </p>
          </div>
        </Container>
      </Section>
    );
  }

  const site = siteQuery.data;

  return (
    <Section labelledBy="integration-preview-title" className="min-h-[48vh]">
      <Container>
        <div aria-live="polite" className="space-y-10">
          <header className="max-w-3xl">
            <StatusBadge>Preview técnico temporário</StatusBadge>
            <h1
              id="integration-preview-title"
              className="mt-4 font-display text-[var(--text-display-md)] font-semibold leading-tight"
            >
              Integração pública validada
            </h1>
            <p className="text-[var(--muted-foreground)]">
              Esta vista confirma contratos, estados e branding. Não é a landing
              visual definitiva.
            </p>
          </header>

          <dl className="grid gap-4 border-y border-border py-6 md:grid-cols-3">
            <div>
              <dt className="text-sm text-[var(--muted-foreground)]">
                Nome comercial
              </dt>
              <dd className="m-0 font-semibold">{site.displayName}</dd>
            </div>
            <div>
              <dt className="text-sm text-[var(--muted-foreground)]">Slug</dt>
              <dd className="m-0 font-semibold">{site.slug}</dd>
            </div>
            <div>
              <dt className="text-sm text-[var(--muted-foreground)]">
                Localização pública
              </dt>
              <dd className="m-0 font-semibold">
                {site.locationLabel ?? "Não configurada"}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-[var(--muted-foreground)]">
                Branding carregado
              </dt>
              <dd className="m-0 font-semibold">
                {site.branding.hasValidPrimaryColor
                  ? "Cor primária válida"
                  : "Fallback de cor"}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-[var(--muted-foreground)]">Logo</dt>
              <dd className="m-0 font-semibold">
                {site.branding.logoUrl ? "Configurado" : "Fallback textual"}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-[var(--muted-foreground)]">
                Footer text
              </dt>
              <dd className="m-0 font-semibold">
                {site.footerText ? "Configurado" : "Não configurado"}
              </dd>
            </div>
          </dl>

          <section
            aria-labelledby="integration-services-title"
            className="space-y-4"
          >
            <div className="flex flex-wrap items-center justify-between gap-4">
              <h2
                id="integration-services-title"
                className="m-0 font-display text-xl font-semibold"
              >
                Serviços públicos ({services.length})
              </h2>
              {servicesQuery.isLoading ? (
                <StatusBadge>A carregar serviços</StatusBadge>
              ) : null}
            </div>
            {servicesQuery.isLoading ? (
              <LoadingBlock label="A carregar serviços públicos" />
            ) : null}
            {servicesQuery.isError ? (
              <PartialError>
                {getErrorMessage(
                  servicesQuery.error,
                  "Não foi possível carregar os serviços públicos.",
                )}
              </PartialError>
            ) : null}
            {!servicesQuery.isLoading &&
            !servicesQuery.isError &&
            services.length === 0 ? (
              <p className="text-[var(--muted-foreground)]">
                Esta empresa ainda não tem serviços ativos publicados.
              </p>
            ) : null}
            {services.length > 0 ? (
              <ul className="divide-y divide-border border-y border-border">
                {services.map((service) => (
                  <li key={service.id} className="py-4">
                    <p className="m-0 font-semibold">{service.name}</p>
                    <p className="m-0 text-sm text-[var(--muted-foreground)]">
                      {service.summary ?? "Sem descrição pública."}
                    </p>
                  </li>
                ))}
              </ul>
            ) : null}
          </section>

          <section
            aria-labelledby="integration-gallery-title"
            className="space-y-4"
          >
            <div className="flex flex-wrap items-center justify-between gap-4">
              <h2
                id="integration-gallery-title"
                className="m-0 font-display text-xl font-semibold"
              >
                Galeria pública ({gallery.length})
              </h2>
              {galleryQuery.isLoading ? (
                <StatusBadge>A carregar galeria</StatusBadge>
              ) : null}
            </div>
            {galleryQuery.isLoading ? (
              <LoadingBlock label="A carregar galeria pública" />
            ) : null}
            {galleryQuery.isError ? (
              <PartialError>
                {getErrorMessage(
                  galleryQuery.error,
                  "Não foi possível carregar a galeria pública.",
                )}
              </PartialError>
            ) : null}
            {!galleryQuery.isLoading &&
            !galleryQuery.isError &&
            gallery.length === 0 ? (
              <p className="text-[var(--muted-foreground)]">
                Esta empresa ainda não tem itens ativos na galeria.
              </p>
            ) : null}
            {gallery.length > 0 ? (
              <>
                <p className="text-sm text-[var(--muted-foreground)]">
                  Pares before/after completos: {completePairs.length}
                </p>
                <ul className="divide-y divide-border border-y border-border">
                  {gallery.map((item) => (
                    <li key={item.id} className="py-4">
                      <p className="m-0 font-semibold">{item.title}</p>
                      <p className="m-0 text-sm text-[var(--muted-foreground)]">
                        Before: {item.hasBeforeImage ? "disponível" : "ausente"}{" "}
                        · After: {item.hasAfterImage ? "disponível" : "ausente"}{" "}
                        · Par completo:{" "}
                        {item.hasCompleteBeforeAfterPair ? "sim" : "não"}
                      </p>
                    </li>
                  ))}
                </ul>
              </>
            ) : null}
          </section>
        </div>
      </Container>
    </Section>
  );
}
