"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { ApiError } from "@/lib/api/errors";
import { login as loginRequest, me } from "../api/auth-api";
import {
  clearAuthSession,
  getAccessToken,
  persistAuthSession,
} from "../api/auth-storage";
import type { AuthResponse, LoginFormValues, MeResponse } from "../types/auth";

type AuthContextValue = {
  accessToken: string | null;
  session: MeResponse | null;
  isAuthenticated: boolean;
  isCheckingSession: boolean;
  login: (values: LoginFormValues) => Promise<AuthResponse>;
  logout: () => void;
  refetchSession: () => Promise<unknown>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    getAccessToken(),
  );

  const sessionQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => me(accessToken ?? ""),
    enabled: Boolean(accessToken),
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.status === 401) {
        return false;
      }

      return failureCount < 1;
    },
  });

  const loginMutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (auth) => {
      persistAuthSession(auth);
      setAccessToken(auth.accessToken);
      queryClient.setQueryData(["auth", "me"], {
        user: auth.user,
        company: auth.company,
        branding: null,
        settings: null,
      });
    },
  });

  const logout = useCallback(() => {
    clearAuthSession();
    setAccessToken(null);
    queryClient.removeQueries({ queryKey: ["auth"] });
    queryClient.removeQueries({ queryKey: ["dashboard"] });
    router.replace("/login");
  }, [queryClient, router]);

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      session: sessionQuery.data ?? null,
      isAuthenticated: Boolean(accessToken && sessionQuery.data),
      isCheckingSession:
        Boolean(accessToken) &&
        (sessionQuery.isLoading || sessionQuery.isFetching),
      login: loginMutation.mutateAsync,
      logout,
      refetchSession: sessionQuery.refetch,
    }),
    [
      accessToken,
      loginMutation.mutateAsync,
      logout,
      sessionQuery.data,
      sessionQuery.isFetching,
      sessionQuery.isLoading,
      sessionQuery.refetch,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
