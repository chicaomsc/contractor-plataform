import { apiRequest } from "@/lib/api/http-client";
import { ApiError } from "@/lib/api/errors";
import type { z } from "zod";
import {
  publicGalleryDtoSchema,
  publicServicesDtoSchema,
  publicSiteDtoSchema,
} from "../types/api";

type PublicSiteRequestOptions = {
  signal?: AbortSignal;
};

const encodeSlug = (companySlug: string) => encodeURIComponent(companySlug);

function parseApiResponse<T>(schema: z.ZodType<T>, data: unknown): T {
  const result = schema.safeParse(data);
  if (!result.success) {
    throw new ApiError(
      "A resposta da API não tem o formato esperado.",
      0,
      null,
      "invalid-response",
    );
  }

  return result.data;
}

export async function fetchPublicSite(
  companySlug: string,
  options: PublicSiteRequestOptions = {},
) {
  const data = await apiRequest<unknown>(
    `/public/sites/${encodeSlug(companySlug)}`,
    {
      signal: options.signal,
    },
  );
  return parseApiResponse(publicSiteDtoSchema, data);
}

export async function fetchPublicServices(
  companySlug: string,
  options: PublicSiteRequestOptions = {},
) {
  const data = await apiRequest<unknown>(
    `/public/sites/${encodeSlug(companySlug)}/services`,
    { signal: options.signal },
  );
  return parseApiResponse(publicServicesDtoSchema, data);
}

export async function fetchPublicGallery(
  companySlug: string,
  options: PublicSiteRequestOptions = {},
) {
  const data = await apiRequest<unknown>(
    `/public/sites/${encodeSlug(companySlug)}/gallery`,
    { signal: options.signal },
  );
  return parseApiResponse(publicGalleryDtoSchema, data);
}
