import { CompanyDetailPage } from "@/features/platform-admin/components/CompanyDetailPage";

export default function AdminCompanyDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return params.then(({ id }) => <CompanyDetailPage companyId={id} />);
}
