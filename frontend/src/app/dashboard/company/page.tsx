import { CompanyPage } from "@/features/dashboard/components/CompanyPage";
import { RequireCompanyView } from "@/features/auth/components/PermissionGuard";

export const metadata = {
  title: "Empresa",
};

export default function DashboardCompanyPage() {
  return (
    <RequireCompanyView>
      <CompanyPage />
    </RequireCompanyView>
  );
}
