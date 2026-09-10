import { NewEstimatePage } from "@/features/dashboard/components/estimates/NewEstimatePage";
import { RequireEstimatesAccess } from "@/features/auth/components/PermissionGuard";

export const metadata = { title: "Novo orçamento" };

export default function DashboardNewEstimatePage() {
  return (
    <RequireEstimatesAccess>
      <NewEstimatePage />
    </RequireEstimatesAccess>
  );
}
