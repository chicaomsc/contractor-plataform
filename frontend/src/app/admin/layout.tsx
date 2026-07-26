import type { ReactNode } from "react";
import { AdminGuard } from "@/features/auth/components/AuthGuard";
import { AdminShell } from "@/features/platform-admin/components/AdminShell";

export const metadata = {
  title: "Platform Admin",
};

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <AdminGuard>
      <AdminShell>{children}</AdminShell>
    </AdminGuard>
  );
}
