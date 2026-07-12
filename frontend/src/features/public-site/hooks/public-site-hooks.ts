"use client";

import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/errors";
import {
  fetchPublicGallery,
  fetchPublicServices,
  fetchPublicSite,
} from "../api/public-site-api";
import { publicSiteQueryKeys } from "../api/query-keys";
import {
  mapPublicGalleryItemDto,
  mapPublicServiceDto,
  mapPublicSiteDto,
} from "../mappers/public-site";

const SITE_STALE_TIME_MS = 5 * 60 * 1000;
const COLLECTION_STALE_TIME_MS = 2 * 60 * 1000;

function shouldRetry(failureCount: number, error: Error) {
  if (
    error instanceof ApiError &&
    (error.status === 404 || error.code === "invalid-response")
  ) {
    return false;
  }

  return failureCount < 1;
}

export function usePublicSite(companySlug: string | null) {
  return useQuery({
    queryKey: companySlug
      ? publicSiteQueryKeys.site(companySlug)
      : publicSiteQueryKeys.site("__missing-slug__"),
    queryFn: async ({ signal }) => {
      if (!companySlug) {
        throw new ApiError("Configure o slug público do tenant.", 0);
      }

      const dto = await fetchPublicSite(companySlug, { signal });
      return mapPublicSiteDto(dto);
    },
    enabled: true,
    staleTime: SITE_STALE_TIME_MS,
    retry: shouldRetry,
  });
}

export function usePublicServices(companySlug: string | null, enabled = true) {
  return useQuery({
    queryKey: companySlug
      ? publicSiteQueryKeys.services(companySlug)
      : publicSiteQueryKeys.services("__missing-slug__"),
    queryFn: async ({ signal }) => {
      if (!companySlug) {
        throw new ApiError("Configure o slug público do tenant.", 0);
      }

      const dtos = await fetchPublicServices(companySlug, { signal });
      return dtos.map(mapPublicServiceDto);
    },
    enabled: enabled && Boolean(companySlug),
    staleTime: COLLECTION_STALE_TIME_MS,
    retry: shouldRetry,
  });
}

export function usePublicGallery(companySlug: string | null, enabled = true) {
  return useQuery({
    queryKey: companySlug
      ? publicSiteQueryKeys.gallery(companySlug)
      : publicSiteQueryKeys.gallery("__missing-slug__"),
    queryFn: async ({ signal }) => {
      if (!companySlug) {
        throw new ApiError("Configure o slug público do tenant.", 0);
      }

      const dtos = await fetchPublicGallery(companySlug, { signal });
      return dtos.map(mapPublicGalleryItemDto);
    },
    enabled: enabled && Boolean(companySlug),
    staleTime: COLLECTION_STALE_TIME_MS,
    retry: shouldRetry,
  });
}
