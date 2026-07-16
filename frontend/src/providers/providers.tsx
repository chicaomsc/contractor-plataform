"use client";

import type { ReactNode } from "react";
import { AuthProvider } from "@/features/auth/hooks/auth-context";
import { QueryProvider } from "./query-provider";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryProvider>
      <AuthProvider>{children}</AuthProvider>
    </QueryProvider>
  );
}
