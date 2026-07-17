"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/auth-context";
import {
  changeEstimateStatus,
  createEstimate,
  deleteEstimate,
  fetchEstimate,
  fetchEstimates,
  updateEstimate,
  type EstimateFilters,
} from "../api/estimates-api";
import { dashboardQueryKeys } from "../api/query-keys";
import type {
  CreateEstimateInput,
  EstimateStatus,
  UpdateEstimateInput,
} from "../types/estimates";

function useAccessToken() {
  const { accessToken } = useAuth();

  if (!accessToken) {
    throw new Error("Dashboard hooks require an authenticated session");
  }

  return accessToken;
}

export function useEstimates(filters: EstimateFilters = {}) {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.estimates(filters),
    queryFn: () => fetchEstimates(accessToken, filters),
  });
}

export function useEstimate(estimateId: string) {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.estimate(estimateId),
    queryFn: () => fetchEstimate(accessToken, estimateId),
  });
}

export function useCreateEstimate() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateEstimateInput) =>
      createEstimate(accessToken, payload),
    onSuccess: (estimate) => {
      queryClient.setQueryData(dashboardQueryKeys.estimate(estimate.id), estimate);
      queryClient.invalidateQueries({
        queryKey: [...dashboardQueryKeys.all, "estimates"],
      });
    },
  });
}

export function useUpdateEstimate() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      estimateId,
      payload,
    }: {
      estimateId: string;
      payload: UpdateEstimateInput;
    }) => updateEstimate(accessToken, estimateId, payload),
    onSuccess: (estimate) => {
      queryClient.setQueryData(dashboardQueryKeys.estimate(estimate.id), estimate);
      queryClient.invalidateQueries({
        queryKey: [...dashboardQueryKeys.all, "estimates"],
      });
    },
  });
}

export function useDeleteEstimate() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (estimateId: string) => deleteEstimate(accessToken, estimateId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: [...dashboardQueryKeys.all, "estimates"],
      });
    },
  });
}

export function useChangeEstimateStatus() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      estimateId,
      status,
    }: {
      estimateId: string;
      status: EstimateStatus;
    }) => changeEstimateStatus(accessToken, estimateId, status),
    onSuccess: (estimate) => {
      queryClient.setQueryData(dashboardQueryKeys.estimate(estimate.id), estimate);
      queryClient.invalidateQueries({
        queryKey: [...dashboardQueryKeys.all, "estimates"],
      });
    },
  });
}
