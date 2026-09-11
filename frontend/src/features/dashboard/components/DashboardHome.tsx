"use client";

import {
  Building2,
  ExternalLink,
  Image,
  Images,
  Palette,
  Settings,
  Wrench,
} from "lucide-react";
import Link from "next/link";
import { buildTenantLandingUrl } from "@/lib/tenant/tenant-landing-url";
import { useAuth } from "@/features/auth/hooks/auth-context";
import {
  canManageBranding,
  canManageGallery,
  canManageServices,
  canManageSettings,
} from "@/features/auth/permissions";
import {
  useBranding,
  useCompany,
  useGallery,
  useOnboardingStatus,
  useServices,
  useSettings,
} from "../hooks/dashboard-hooks";
import { formatDateTime, latestIsoDate } from "../utils/forms";
import { ErrorState, LoadingState } from "./DashboardState";
import { PageHeader } from "./PageHeader";
import { OnboardingChecklist } from "./onboarding/OnboardingChecklist";

function StatCard({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: string | number;
  icon: typeof Building2;
}) {
  return (
    <div className="border border-border bg-surface p-5">
      <div className="flex items-center justify-between gap-4">
        <p className="m-0 text-sm font-semibold text-[var(--muted-foreground)]">
          {label}
        </p>
        <Icon size={20} className="text-primary" aria-hidden="true" />
      </div>
      <p className="m-0 mt-4 font-display text-3xl font-bold">{value}</p>
    </div>
  );
}

export function DashboardHome() {
  const { session } = useAuth();
  const role = session?.user.role;
  const canLoadBranding = canManageBranding(role);
  const canLoadSettings = canManageSettings(role);
  const canLoadServices = canManageServices(role);
  const canLoadGallery = canManageGallery(role);
  const isOwner = role === "OWNER";
  const companyQuery = useCompany();
  const brandingQuery = useBranding({ enabled: canLoadBranding });
  const settingsQuery = useSettings({ enabled: canLoadSettings });
  const servicesQuery = useServices({ enabled: canLoadServices });
  const galleryQuery = useGallery({ enabled: canLoadGallery });
  const onboardingQuery = useOnboardingStatus({ enabled: isOwner });

  const queries = [
    companyQuery,
    ...(canLoadBranding ? [brandingQuery] : []),
    ...(canLoadSettings ? [settingsQuery] : []),
    ...(canLoadServices ? [servicesQuery] : []),
    ...(canLoadGallery ? [galleryQuery] : []),
  ];
  const isLoading = queries.some((query) => query.isLoading);
  const hasError = queries.some((query) => query.isError);

  if (isLoading) {
    return <LoadingState label="A carregar dashboard" />;
  }

  if (hasError) {
    return (
      <ErrorState
        title="Não foi possível carregar o dashboard"
        description="Os dados administrativos vêm da API autenticada. Tente novamente."
        onRetry={() => {
          queries.forEach((query) => void query.refetch());
        }}
      />
    );
  }

  const company = companyQuery.data;
  const branding = brandingQuery.data;
  const settings = settingsQuery.data;
  const services = canLoadServices ? (servicesQuery.data ?? []) : [];
  const gallery = canLoadGallery ? (galleryQuery.data ?? []) : [];
  const publicSiteUrl = company?.slug
    ? buildTenantLandingUrl(company.slug)
    : null;
  const lastUpdated = latestIsoDate([
    ...services.map((service) => service.updatedAt),
    ...gallery.map((item) => item.updatedAt),
  ]);

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Dashboard"
        title="Visão geral"
        description="Estado administrativo da empresa e da landing pública sem métricas fictícias."
      />

      {isOwner && onboardingQuery.data ? (
        <OnboardingChecklist status={onboardingQuery.data} />
      ) : null}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Empresa"
          value={company?.name ?? "Sem empresa"}
          icon={Building2}
        />
        {canLoadServices ? (
          <StatCard
            label="Serviços publicados"
            value={services.length}
            icon={Wrench}
          />
        ) : null}
        {canLoadGallery ? (
          <StatCard label="Imagens" value={gallery.length} icon={Image} />
        ) : null}
        <StatCard
          label="Status do site"
          value={company?.status ?? "Indefinido"}
          icon={Palette}
        />
      </section>

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="border border-border bg-surface p-6">
          <h2 className="m-0 font-display text-2xl font-semibold">
            Dados principais
          </h2>
          <dl className="mt-6 grid gap-5 md:grid-cols-2">
            <div>
              <dt className="text-sm font-semibold text-[var(--muted-foreground)]">
                Slug público
              </dt>
              <dd className="m-0 mt-1 font-semibold">{company?.slug}</dd>
            </div>
            <div>
              <dt className="text-sm font-semibold text-[var(--muted-foreground)]">
                Email
              </dt>
              <dd className="m-0 mt-1 font-semibold">
                {company?.email ?? "Não configurado"}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-semibold text-[var(--muted-foreground)]">
                Branding
              </dt>
              <dd className="m-0 mt-1 font-semibold">
                {canLoadBranding
                  ? branding?.primaryColor
                    ? "Configurado"
                    : "Parcial"
                  : "Sem acesso"}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-semibold text-[var(--muted-foreground)]">
                Moeda
              </dt>
              <dd className="m-0 mt-1 font-semibold">
                {canLoadSettings
                  ? (settings?.defaultCurrency ?? "Não configurada")
                  : "Sem acesso"}
              </dd>
            </div>
            <div className="md:col-span-2">
              <dt className="text-sm font-semibold text-[var(--muted-foreground)]">
                Última atualização de serviços/galeria
              </dt>
              <dd className="m-0 mt-1 font-semibold">
                {formatDateTime(lastUpdated)}
              </dd>
            </div>
          </dl>
        </div>

        <div className="border border-border bg-surface p-6">
          <h2 className="m-0 font-display text-2xl font-semibold">
            Próximas ações
          </h2>
          <div className="mt-6 space-y-3">
            {publicSiteUrl ? (
              <a
                href={publicSiteUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 border border-primary bg-primary px-4 py-3 text-sm font-bold text-primary-foreground no-underline transition-colors hover:bg-primary-hover"
              >
                <ExternalLink size={18} aria-hidden="true" />
                Visualizar site
              </a>
            ) : null}
            <Link
              href="/dashboard/company"
              className="flex items-center gap-3 border border-border px-4 py-3 text-sm font-semibold no-underline hover:border-primary"
            >
              <Building2 size={18} aria-hidden="true" />
              Editar empresa
            </Link>
            {canLoadBranding ? (
              <Link
                href="/dashboard/branding"
                className="flex items-center gap-3 border border-border px-4 py-3 text-sm font-semibold no-underline hover:border-primary"
              >
                <Palette size={18} aria-hidden="true" />
                Rever branding
              </Link>
            ) : null}
            {canLoadServices ? (
              <Link
                href="/dashboard/services"
                className="flex items-center gap-3 border border-border px-4 py-3 text-sm font-semibold no-underline hover:border-primary"
              >
                <Wrench size={18} aria-hidden="true" />
                Gerenciar serviços
              </Link>
            ) : null}
            {canLoadGallery ? (
              <Link
                href="/dashboard/gallery"
                className="flex items-center gap-3 border border-border px-4 py-3 text-sm font-semibold no-underline hover:border-primary"
              >
                <Images size={18} aria-hidden="true" />
                Gerenciar galeria
              </Link>
            ) : null}
            {canLoadSettings ? (
              <Link
                href="/dashboard/settings"
                className="flex items-center gap-3 border border-border px-4 py-3 text-sm font-semibold no-underline hover:border-primary"
              >
                <Settings size={18} aria-hidden="true" />
                Ajustar settings
              </Link>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  );
}
