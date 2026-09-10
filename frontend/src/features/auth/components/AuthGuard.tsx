"use client";

import { Loader2 } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { ApiError } from "@/lib/api/errors";
import { canAccessDashboard, canAccessPlatformAdmin } from "../permissions";
import { useAuth } from "../hooks/auth-context";

export function AuthGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const {
    accessToken,
    isAuthenticated,
    isCheckingSession,
    isSessionExpired,
    session,
    logout,
  } = useAuth();

  useEffect(() => {
    if (!accessToken && !isSessionExpired) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [accessToken, isSessionExpired, pathname, router]);

  useEffect(() => {
    if (!session && !isCheckingSession && accessToken && !isSessionExpired) {
      void logout();
    }
  }, [accessToken, isCheckingSession, isSessionExpired, logout, session]);

  useEffect(() => {
    if (canAccessPlatformAdmin(session?.user.role)) {
      router.replace("/admin");
    }
  }, [router, session?.user.role]);

  if (
    !accessToken ||
    isCheckingSession ||
    !isAuthenticated ||
    !canAccessDashboard(session?.user.role) ||
    !session.company
  ) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6">
        <div className="inline-flex items-center gap-3 text-sm font-semibold text-[var(--muted-foreground)]">
          <Loader2 size={18} className="animate-spin" aria-hidden="true" />A
          validar sessão
        </div>
      </main>
    );
  }

  return children;
}

export function AdminGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const {
    accessToken,
    isAuthenticated,
    isCheckingSession,
    isSessionExpired,
    session,
    logout,
  } = useAuth();

  useEffect(() => {
    if (!accessToken && !isSessionExpired) {
      router.replace(`/admin/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [accessToken, isSessionExpired, pathname, router]);

  useEffect(() => {
    if (!session && !isCheckingSession && accessToken && !isSessionExpired) {
      void logout();
    }
  }, [accessToken, isCheckingSession, isSessionExpired, logout, session]);

  useEffect(() => {
    if (canAccessDashboard(session?.user.role)) {
      router.replace("/dashboard");
    }
  }, [router, session?.user.role]);

  if (
    !accessToken ||
    isCheckingSession ||
    !isAuthenticated ||
    !canAccessPlatformAdmin(session?.user.role)
  ) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6">
        <div className="inline-flex items-center gap-3 text-sm font-semibold text-[var(--muted-foreground)]">
          <Loader2 size={18} className="animate-spin" aria-hidden="true" />A
          validar sessão
        </div>
      </main>
    );
  }

  return children;
}

export function isUnauthorizedError(error: unknown) {
  return error instanceof ApiError && error.status === 401;
}
