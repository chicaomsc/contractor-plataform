import { GalleryPage } from "@/features/dashboard/components/gallery/GalleryPage";
import { RequireGalleryManagement } from "@/features/auth/components/PermissionGuard";

export default function DashboardGalleryPage() {
  return (
    <RequireGalleryManagement>
      <GalleryPage />
    </RequireGalleryManagement>
  );
}
