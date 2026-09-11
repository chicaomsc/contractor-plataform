"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import {
  login as loginRequest,
  logout as logoutRequest,
  me,
} from "../api/auth-api";
import {
  clearAuthSession,
  getAccessToken,
  getRefreshToken,
  persistAuthSession,
} from "../api/auth-storage";
import {
  hasSessionExpired,
  markSessionActive,
  subscribeToSessionExpired,
  subscribeToSessionRefresh,
} from "../api/session-lifecycle";
import type { AuthResponse, LoginFormValues, MeResponse } from "../types/auth";

type AuthContextValue = {
  accessToken: string | null;
  session: MeResponse | null;
  isAuthenticated: boolean;
  isCheckingSession: boolean;
  isSessionExpired: boolean;
  login: (values: LoginFormValues) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refetchSession: () => Promise<unknown>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const loginButtonRef = useRef<HTMLButtonElement | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    getAccessToken(),
  );
  const [isSessionExpired, setIsSessionExpired] = useState(false);

  useEffect(() => {
    const unsubscribeRefresh = subscribeToSessionRefresh((auth) => {
      setAccessToken(auth.accessToken);
      queryClient.setQueryData(["auth", "me"], {
        user: auth.user,
        company: auth.company,
        branding: null,
        settings: null,
      });
    });

    const unsubscribeExpired = subscribeToSessionExpired(() => {
      setAccessToken(null);
      queryClient.removeQueries({ queryKey: ["auth"] });
      queryClient.removeQueries({ queryKey: ["dashboard"] });
      queryClient.removeQueries({ queryKey: ["platform-admin"] });

      const pathname = window.location.pathname;
      if (!isAuthEntryPath(pathname)) {
        setIsSessionExpired(true);
      }
    });

    return () => {
      unsubscribeRefresh();
      unsubscribeExpired();
    };
  }, [queryClient]);

  useEffect(() => {
    if (isSessionExpired) {
      loginButtonRef.current?.focus();
    }
  }, [isSessionExpired]);

  const sessionQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => me(accessToken ?? ""),
    enabled: Boolean(accessToken),
    retry: (failureCount, error) => {
      if (hasSessionExpired()) {
        return false;
      }

      if (error instanceof ApiError && error.status === 401) {
        return false;
      }

      return failureCount < 1;
    },
  });

  const loginMutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (auth) => {
      markSessionActive();
      persistAuthSession(auth);
      setIsSessionExpired(false);
      setAccessToken(auth.accessToken);
      queryClient.setQueryData(["auth", "me"], {
        user: auth.user,
        company: auth.company,
        branding: null,
        settings: null,
      });
    },
  });

  const logout = useCallback(async () => {
    const currentAccessToken = getAccessToken();
    const currentRefreshToken = getRefreshToken();

    if (currentAccessToken && currentRefreshToken) {
      try {
        await logoutRequest({
          accessToken: currentAccessToken,
          refreshToken: currentRefreshToken,
        });
      } catch {
        // Best-effort: the local session is cleared below regardless of whether the
        // backend call succeeded (offline, expired access token, server error, ...).
      }
    }

    clearAuthSession();
    markSessionActive();
    setIsSessionExpired(false);
    setAccessToken(null);
    queryClient.removeQueries({ queryKey: ["auth"] });
    queryClient.removeQueries({ queryKey: ["dashboard"] });
    queryClient.removeQueries({ queryKey: ["platform-admin"] });
    router.replace("/login");
  }, [queryClient, router]);

  const goToLogin = useCallback(() => {
    markSessionActive();
    setIsSessionExpired(false);
    router.replace("/login");
  }, [router]);

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      session: sessionQuery.data ?? null,
      isAuthenticated: Boolean(accessToken && sessionQuery.data),
      isSessionExpired,
      isCheckingSession:
        Boolean(accessToken) &&
        (sessionQuery.isLoading || sessionQuery.isFetching),
      login: loginMutation.mutateAsync,
      logout,
      refetchSession: sessionQuery.refetch,
    }),
    [
      accessToken,
      isSessionExpired,
      loginMutation.mutateAsync,
      logout,
      sessionQuery.data,
      sessionQuery.isFetching,
      sessionQuery.isLoading,
      sessionQuery.refetch,
    ],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      {isSessionExpired ? (
        <div
          className="fixed inset-0 z-[90] flex items-center justify-center bg-black/45 px-4"
          role="presentation"
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="session-expired-title"
            aria-describedby="session-expired-description"
            className="w-full max-w-md border border-border bg-surface p-6 shadow-sm"
          >
            <h2
              id="session-expired-title"
              className="m-0 font-display text-2xl font-semibold"
            >
              Sua sessão expirou
            </h2>
            <p
              id="session-expired-description"
              className="m-0 mt-3 text-sm text-[var(--muted-foreground)]"
            >
              Por segurança, sua sessão foi encerrada. Entre novamente para
              continuar.
            </p>
            <div className="mt-8 flex justify-end">
              <Button ref={loginButtonRef} type="button" onClick={goToLogin}>
                Ir para o login
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}

function isAuthEntryPath(pathname: string) {
  return (
    pathname === "/login" ||
    pathname === "/admin/login" ||
    pathname === "/forgot-password" ||
    pathname === "/reset-password" ||
    pathname === "/invite" ||
    pathname === "/invite/team"
  );
}
