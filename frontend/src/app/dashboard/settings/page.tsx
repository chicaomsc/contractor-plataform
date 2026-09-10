import { SettingsPage } from "@/features/dashboard/components/SettingsPage";
import { RequireSettingsManagement } from "@/features/auth/components/PermissionGuard";

export const metadata = {
  title: "Settings",
};

export default function DashboardSettingsPage() {
  return (
    <RequireSettingsManagement>
      <SettingsPage />
    </RequireSettingsManagement>
  );
}
