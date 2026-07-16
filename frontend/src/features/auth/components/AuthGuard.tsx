"use client";

import { Loader2 } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { ApiError } from "@/lib/api/errors";
import { useAuth } from "../hooks/auth-context";

export function AuthGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { accessToken, isAuthenticated, isCheckingSession, session, logout } =
    useAuth();

  useEffect(() => {
    if (!accessToken) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [accessToken, pathname, router]);

  useEffect(() => {
    if (!session && !isCheckingSession && accessToken) {
      logout();
    }
  }, [accessToken, isCheckingSession, logout, session]);

  if (!accessToken || isCheckingSession || !isAuthenticated) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6">
        <div className="inline-flex items-center gap-3 text-sm font-semibold text-[var(--muted-foreground)]">
          <Loader2 size={18} className="animate-spin" aria-hidden="true" />
          A validar sessão
        </div>
      </main>
    );
  }

  return children;
}

export function isUnauthorizedError(error: unknown) {
  return error instanceof ApiError && error.status === 401;
}
