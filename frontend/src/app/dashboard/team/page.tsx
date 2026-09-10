import { RequireTeamManagement } from "@/features/auth/components/PermissionGuard";
import { TeamPage } from "@/features/dashboard/components/team/TeamPage";

export const metadata = {
  title: "Equipe",
};

export default function DashboardTeamPage() {
  return (
    <RequireTeamManagement>
      <TeamPage />
    </RequireTeamManagement>
  );
}
