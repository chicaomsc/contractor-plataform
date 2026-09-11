import { EstimateDetailPage } from "@/features/dashboard/components/estimates/EstimateDetailPage";
import { RequireEstimatesAccess } from "@/features/auth/components/PermissionGuard";

export const metadata = { title: "Orçamento" };

export default async function DashboardEstimateDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return (
    <RequireEstimatesAccess>
      <EstimateDetailPage estimateId={id} />
    </RequireEstimatesAccess>
  );
}
