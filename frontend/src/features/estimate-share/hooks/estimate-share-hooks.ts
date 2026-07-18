"use client";

import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/errors";
import { fetchPublicEstimateShare } from "../api/estimate-share-api";

function shouldRetry(failureCount: number, error: Error) {
  if (error instanceof ApiError && (error.status === 404 || error.code === "invalid-response")) {
    return false;
  }
  return failureCount < 1;
}

export function usePublicEstimateShare(token: string) {
  return useQuery({
    queryKey: ["estimate-share", token] as const,
    queryFn: ({ signal }) => fetchPublicEstimateShare(token, { signal }),
    retry: shouldRetry,
  });
}
