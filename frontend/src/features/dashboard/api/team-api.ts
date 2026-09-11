import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  createTeamInvitationSchema,
  teamInvitationSchema,
  teamInvitationsSchema,
  teamMemberSchema,
  teamMembersSchema,
  updateTeamMemberRoleSchema,
  type CreateTeamInvitationInput,
  type TeamInvitation,
  type TeamMember,
  type UpdateTeamMemberRoleInput,
} from "../types/team";

export async function fetchTeamMembers(
  accessToken: string,
): Promise<TeamMember[]> {
  const response = await adminApiRequest<unknown>("/team/members", {
    accessToken,
  });
  return teamMembersSchema.parse(response);
}

export async function updateTeamMemberRole(
  accessToken: string,
  userId: string,
  payload: UpdateTeamMemberRoleInput,
): Promise<TeamMember> {
  const body = updateTeamMemberRoleSchema.parse(payload);
  const response = await adminApiRequest<unknown>(
    `/team/members/${userId}/role`,
    {
      method: "PATCH",
      accessToken,
      body: JSON.stringify(body),
    },
  );
  return teamMemberSchema.parse(response);
}

export async function removeTeamMember(
  accessToken: string,
  userId: string,
): Promise<void> {
  await adminApiRequest<void>(`/team/members/${userId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function fetchTeamInvitations(
  accessToken: string,
): Promise<TeamInvitation[]> {
  const response = await adminApiRequest<unknown>("/team/invitations", {
    accessToken,
  });
  return teamInvitationsSchema.parse(response);
}

export async function createTeamInvitation(
  accessToken: string,
  payload: CreateTeamInvitationInput,
): Promise<TeamInvitation> {
  const body = createTeamInvitationSchema.parse(payload);
  const response = await adminApiRequest<unknown>("/team/invitations", {
    method: "POST",
    accessToken,
    body: JSON.stringify(body),
  });
  return teamInvitationSchema.parse(response);
}

export async function resendTeamInvitation(
  accessToken: string,
  invitationId: string,
): Promise<TeamInvitation> {
  const response = await adminApiRequest<unknown>(
    `/team/invitations/${invitationId}/resend`,
    {
      method: "POST",
      accessToken,
    },
  );
  return teamInvitationSchema.parse(response);
}

export async function revokeTeamInvitation(
  accessToken: string,
  invitationId: string,
): Promise<void> {
  await adminApiRequest<void>(`/team/invitations/${invitationId}`, {
    method: "DELETE",
    accessToken,
  });
}
