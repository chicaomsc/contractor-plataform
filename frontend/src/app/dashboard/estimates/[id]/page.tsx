import { EstimateDetailPage } from "@/features/dashboard/components/estimates/EstimateDetailPage";

export const metadata = { title: "Orçamento" };

export default async function DashboardEstimateDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <EstimateDetailPage estimateId={id} />;
}
