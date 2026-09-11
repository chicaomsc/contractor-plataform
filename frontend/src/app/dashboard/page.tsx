import { DashboardHome } from "@/features/dashboard/components/DashboardHome";
import { RequireDashboardAccess } from "@/features/auth/components/PermissionGuard";

export default function DashboardPage() {
  return (
    <RequireDashboardAccess>
      <DashboardHome />
    </RequireDashboardAccess>
  );
}
