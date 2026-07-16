"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/auth-context";
import {
  fetchBranding,
  fetchCompany,
  fetchGallery,
  fetchServices,
  fetchSettings,
  updateBranding,
  updateCompany,
  updateSettings,
} from "../api/dashboard-api";
import { dashboardQueryKeys } from "../api/query-keys";
import type {
  UpdateBrandingInput,
  UpdateCompanyInput,
  UpdateSettingsInput,
} from "../types/admin";

function useAccessToken() {
  const { accessToken } = useAuth();

  if (!accessToken) {
    throw new Error("Dashboard hooks require an authenticated session");
  }

  return accessToken;
}

export function useCompany() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.company(),
    queryFn: () => fetchCompany(accessToken),
  });
}

export function useUpdateCompany() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateCompanyInput) =>
      updateCompany(accessToken, payload),
    onSuccess: (company) => {
      queryClient.setQueryData(dashboardQueryKeys.company(), company);
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
}

export function useBranding() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.branding(),
    queryFn: () => fetchBranding(accessToken),
  });
}

export function useUpdateBranding() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateBrandingInput) =>
      updateBranding(accessToken, payload),
    onSuccess: (branding) => {
      queryClient.setQueryData(dashboardQueryKeys.branding(), branding);
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
}

export function useSettings() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.settings(),
    queryFn: () => fetchSettings(accessToken),
  });
}

export function useUpdateSettings() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateSettingsInput) =>
      updateSettings(accessToken, payload),
    onSuccess: (settings) => {
      queryClient.setQueryData(dashboardQueryKeys.settings(), settings);
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
}

export function useServices() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.services(),
    queryFn: () => fetchServices(accessToken),
  });
}

export function useGallery() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.gallery(),
    queryFn: () => fetchGallery(accessToken),
  });
}
