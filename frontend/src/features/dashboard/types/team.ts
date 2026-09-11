import { z } from "zod";
import { userStatusSchema } from "@/features/auth/types/auth";

export const teamAssignableRoleSchema = z.enum(["MANAGER", "MEMBER"]);
export const teamMemberRoleSchema = z.enum(["OWNER", "MANAGER", "MEMBER"]);
export const teamInvitationStatusSchema = z.enum([
  "PENDING",
  "EXPIRED",
  "USED",
  "REVOKED",
]);

export const teamMemberSchema = z
  .object({
    id: z.string(),
    name: z.string(),
    email: z.string().email(),
    role: teamMemberRoleSchema,
    status: userStatusSchema,
    createdAt: z.string(),
  })
  .strict();

export const teamInvitationSchema = z
  .object({
    id: z.string(),
    email: z.string().email(),
    role: teamAssignableRoleSchema,
    status: teamInvitationStatusSchema,
    expiresAt: z.string(),
    createdAt: z.string(),
  })
  .strict();

export const teamMembersSchema = z.array(teamMemberSchema);
export const teamInvitationsSchema = z.array(teamInvitationSchema);

export const createTeamInvitationSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Indique o email do colaborador.")
    .pipe(z.string().email("Indique um email válido.")),
  role: teamAssignableRoleSchema,
});

export const updateTeamMemberRoleSchema = z.object({
  role: teamAssignableRoleSchema,
});

export type TeamAssignableRole = z.infer<typeof teamAssignableRoleSchema>;
export type TeamMember = z.infer<typeof teamMemberSchema>;
export type TeamInvitation = z.infer<typeof teamInvitationSchema>;
export type TeamInvitationStatus = z.infer<typeof teamInvitationStatusSchema>;
export type CreateTeamInvitationInput = z.infer<
  typeof createTeamInvitationSchema
>;
export type UpdateTeamMemberRoleInput = z.infer<
  typeof updateTeamMemberRoleSchema
>;
