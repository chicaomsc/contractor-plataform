"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/auth-context";
import {
  createGalleryItem,
  deleteGalleryImage,
  fetchBranding,
  fetchCompany,
  fetchGallery,
  fetchServices,
  fetchSettings,
  createService,
  deleteGalleryItem,
  deleteService,
  featureGalleryItem,
  reorderGalleryItem,
  reorderService,
  updateGalleryItem,
  updateService,
  updateBranding,
  updateCompany,
  updateSettings,
  uploadGalleryImage,
} from "../api/dashboard-api";
import { dashboardQueryKeys } from "../api/query-keys";
import type {
  UpdateBrandingInput,
  UpdateCompanyInput,
  GalleryFormInput,
  ServiceFormInput,
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

export function useCreateService() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ServiceFormInput) =>
      createService(accessToken, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.services() });
    },
  });
}

export function useUpdateService() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      serviceId,
      payload,
    }: {
      serviceId: string;
      payload: ServiceFormInput;
    }) => updateService(accessToken, serviceId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.services() });
    },
  });
}

export function useDeleteService() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (serviceId: string) => deleteService(accessToken, serviceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.services() });
    },
  });
}

export function useReorderService() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      serviceId,
      displayOrder,
    }: {
      serviceId: string;
      displayOrder: number;
    }) => reorderService(accessToken, serviceId, displayOrder),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.services() });
    },
  });
}

export function useGallery() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.gallery(),
    queryFn: () => fetchGallery(accessToken),
  });
}

export function useCreateGalleryItem() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: GalleryFormInput) =>
      createGalleryItem(accessToken, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}

export function useUpdateGalleryItem() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      galleryItemId,
      payload,
    }: {
      galleryItemId: string;
      payload: GalleryFormInput;
    }) => updateGalleryItem(accessToken, galleryItemId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}

export function useDeleteGalleryItem() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (galleryItemId: string) =>
      deleteGalleryItem(accessToken, galleryItemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}

export function useFeatureGalleryItem() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      galleryItemId,
      featured,
    }: {
      galleryItemId: string;
      featured: boolean;
    }) => featureGalleryItem(accessToken, galleryItemId, featured),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}

export function useReorderGalleryItem() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      galleryItemId,
      displayOrder,
    }: {
      galleryItemId: string;
      displayOrder: number;
    }) => reorderGalleryItem(accessToken, galleryItemId, displayOrder),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}

export function useUploadGalleryImage() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      galleryItemId,
      slot,
      file,
    }: {
      galleryItemId: string;
      slot: "before" | "after";
      file: File;
    }) => uploadGalleryImage(accessToken, galleryItemId, slot, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}

export function useDeleteGalleryImage() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      galleryItemId,
      slot,
    }: {
      galleryItemId: string;
      slot: "before" | "after";
    }) => deleteGalleryImage(accessToken, galleryItemId, slot),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.gallery() });
    },
  });
}
