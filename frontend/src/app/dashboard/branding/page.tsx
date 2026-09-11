import { BrandingPage } from "@/features/dashboard/components/BrandingPage";
import { RequireBrandingManagement } from "@/features/auth/components/PermissionGuard";

export const metadata = {
  title: "Branding",
};

export default function DashboardBrandingPage() {
  return (
    <RequireBrandingManagement>
      <BrandingPage />
    </RequireBrandingManagement>
  );
}
