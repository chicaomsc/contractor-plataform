import { EstimatesPage } from "@/features/dashboard/components/estimates/EstimatesPage";
import { RequireEstimatesAccess } from "@/features/auth/components/PermissionGuard";

export const metadata = { title: "Orçamentos" };

export default function DashboardEstimatesPage() {
  return (
    <RequireEstimatesAccess>
      <EstimatesPage />
    </RequireEstimatesAccess>
  );
}
