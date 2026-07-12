import { apiRequest } from "@/lib/api/http-client";
import type {
  PublicGalleryItem,
  PublicService,
  PublicSite,
} from "@/types/public-site";

const encodeSlug = (companySlug: string) => encodeURIComponent(companySlug);

export function getPublicSite(companySlug: string) {
  return apiRequest<PublicSite>(`/public/sites/${encodeSlug(companySlug)}`);
}

export function getPublicServices(companySlug: string) {
  return apiRequest<PublicService[]>(
    `/public/sites/${encodeSlug(companySlug)}/services`,
  );
}

export function getPublicGallery(companySlug: string) {
  return apiRequest<PublicGalleryItem[]>(
    `/public/sites/${encodeSlug(companySlug)}/gallery`,
  );
}
