import { ServicesPage } from "@/features/dashboard/components/services/ServicesPage";
import { RequireServicesManagement } from "@/features/auth/components/PermissionGuard";

export const metadata = {
  title: "Serviços",
};

export default function DashboardServicesPage() {
  return (
    <RequireServicesManagement>
      <ServicesPage />
    </RequireServicesManagement>
  );
}
