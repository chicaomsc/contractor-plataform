import type { ReactNode } from "react";
import { AuthGuard } from "@/features/auth/components/AuthGuard";
import { DashboardShell } from "@/features/dashboard/components/DashboardShell";

export const metadata = {
  title: "Dashboard",
};

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return (
    <AuthGuard>
      <DashboardShell>{children}</DashboardShell>
    </AuthGuard>
  );
}
