"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { createCustomer, fetchCustomers } from "../api/customers-api";
import { dashboardQueryKeys } from "../api/query-keys";
import type { QuickCustomerFormInput } from "../types/customers";

function useAccessToken() {
  const { accessToken } = useAuth();

  if (!accessToken) {
    throw new Error("Dashboard hooks require an authenticated session");
  }

  return accessToken;
}

export function useCustomers() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.customers(),
    queryFn: () => fetchCustomers(accessToken),
  });
}

export function useCreateCustomer() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: QuickCustomerFormInput) =>
      createCustomer(accessToken, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: dashboardQueryKeys.customers() });
    },
  });
}
