"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/auth-context";
import {
  createTeamInvitation,
  fetchTeamInvitations,
  fetchTeamMembers,
  removeTeamMember,
  resendTeamInvitation,
  revokeTeamInvitation,
  updateTeamMemberRole,
} from "../api/team-api";
import { dashboardQueryKeys } from "../api/query-keys";
import type {
  CreateTeamInvitationInput,
  TeamAssignableRole,
} from "../types/team";

function useAccessToken() {
  const { accessToken } = useAuth();

  if (!accessToken) {
    throw new Error("Team hooks require an authenticated session");
  }

  return accessToken;
}

export function useTeamMembers() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.teamMembers(),
    queryFn: () => fetchTeamMembers(accessToken),
  });
}

export function useTeamInvitations() {
  const accessToken = useAccessToken();

  return useQuery({
    queryKey: dashboardQueryKeys.teamInvitations(),
    queryFn: () => fetchTeamInvitations(accessToken),
  });
}

export function useCreateTeamInvitation() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateTeamInvitationInput) =>
      createTeamInvitation(accessToken, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: dashboardQueryKeys.teamInvitations(),
      });
    },
  });
}

export function useResendTeamInvitation() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) =>
      resendTeamInvitation(accessToken, invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: dashboardQueryKeys.teamInvitations(),
      });
    },
  });
}

export function useRevokeTeamInvitation() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) =>
      revokeTeamInvitation(accessToken, invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: dashboardQueryKeys.teamInvitations(),
      });
    },
  });
}

export function useUpdateTeamMemberRole() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      userId,
      role,
    }: {
      userId: string;
      role: TeamAssignableRole;
    }) => updateTeamMemberRole(accessToken, userId, { role }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: dashboardQueryKeys.teamMembers(),
      });
    },
  });
}

export function useRemoveTeamMember() {
  const accessToken = useAccessToken();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userId: string) => removeTeamMember(accessToken, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: dashboardQueryKeys.teamMembers(),
      });
    },
  });
}
