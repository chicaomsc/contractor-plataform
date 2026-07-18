"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { ApiError } from "@/lib/api/errors";
import {
  createEstimateShare,
  fetchEstimateShare,
  revokeEstimateShare,
} from "../api/estimate-shares-api";
import { dashboardQueryKeys } from "../api/query-keys";
import type { CreateEstimateShareInput } from "../types/estimate-shares";

function useAccessToken() {
  const { accessToken } = useAuth();

  if (!accessToken) {
    throw new Error("Dashboard hooks require an authenticated session");
  }

  return accessToken;
}

/** 404 means "no share has ever been created for this estimate" — not a fetch failure. */
function shouldRetry(failureCount: number, error: Error) {
  if (error instanceof ApiError && error.status === 404) {
    return false;
  }
  return failureCount < 1;
}

export function useEstimateShare(estimateId: string) {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.estimateShare(estimateId),
    queryFn: () => fetchEstimateShare(accessToken, estimateId),
    retry: shouldRetry,
  });
}

/** Creates (or regenerates, revoking the previous link) a share. Response.token is only ever populated here. */
export function useCreateEstimateShare(estimateId: string) {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateEstimateShareInput = {}) =>
      createEstimateShare(accessToken, estimateId, payload),
    onSuccess: (share) => {
      queryClient.setQueryData(dashboardQueryKeys.estimateShare(estimateId), share);
    },
  });
}

export function useRevokeEstimateShare(estimateId: string) {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => revokeEstimateShare(accessToken, estimateId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.estimateShare(estimateId) });
    },
  });
}
