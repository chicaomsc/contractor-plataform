"use client";

import { Loader2, ShieldAlert } from "lucide-react";
import type { ReactNode } from "react";
import {
  canAccessDashboard,
  canAccessEstimates,
  canManageBranding,
  canManageGallery,
  canManageServices,
  canManageSettings,
  canViewCompany,
} from "../permissions";
import type { UserRole } from "../types/auth";
import { useAuth } from "../hooks/auth-context";

type PermissionGuardProps = {
  canAccess: (role: UserRole) => boolean;
  children: ReactNode;
};

export function AccessDeniedState() {
  return (
    <section
      role="alert"
      className="border border-border bg-surface p-6"
      aria-labelledby="access-denied-title"
    >
      <div className="flex gap-3">
        <ShieldAlert
          size={22}
          className="mt-1 shrink-0 text-[var(--muted-foreground)]"
          aria-hidden="true"
        />
        <div>
          <h1
            id="access-denied-title"
            className="m-0 font-display text-2xl font-semibold"
          >
            Acesso sem permissão
          </h1>
          <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
            Você não tem permissão para acessar esta área.
          </p>
        </div>
      </div>
    </section>
  );
}

function PermissionLoadingState() {
  return (
    <section className="flex min-h-[280px] items-center justify-center border border-border bg-surface">
      <div className="inline-flex items-center gap-3 text-sm font-semibold text-[var(--muted-foreground)]">
        <Loader2 size={18} className="animate-spin" aria-hidden="true" />A
        validar permissões
      </div>
    </section>
  );
}

export function PermissionGuard({ canAccess, children }: PermissionGuardProps) {
  const { session, isCheckingSession } = useAuth();

  if (isCheckingSession || !session) {
    return <PermissionLoadingState />;
  }

  if (!canAccess(session.user.role)) {
    return <AccessDeniedState />;
  }

  return children;
}

export function RequireDashboardAccess({ children }: { children: ReactNode }) {
  return (
    <PermissionGuard canAccess={canAccessDashboard}>{children}</PermissionGuard>
  );
}

export function RequireCompanyView({ children }: { children: ReactNode }) {
  return (
    <PermissionGuard canAccess={canViewCompany}>{children}</PermissionGuard>
  );
}

export function RequireBrandingManagement({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <PermissionGuard canAccess={canManageBranding}>{children}</PermissionGuard>
  );
}

export function RequireSettingsManagement({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <PermissionGuard canAccess={canManageSettings}>{children}</PermissionGuard>
  );
}

export function RequireServicesManagement({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <PermissionGuard canAccess={canManageServices}>{children}</PermissionGuard>
  );
}

export function RequireGalleryManagement({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <PermissionGuard canAccess={canManageGallery}>{children}</PermissionGuard>
  );
}

export function RequireEstimatesAccess({ children }: { children: ReactNode }) {
  return (
    <PermissionGuard canAccess={canAccessEstimates}>{children}</PermissionGuard>
  );
}
