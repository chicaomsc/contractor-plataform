import type { UserRole } from "./types/auth";

export const TENANT_ROLES = ["OWNER", "MANAGER", "MEMBER"] as const;

export const ROLE_LABELS: Record<UserRole, string> = {
  SUPER_ADMIN: "Administrador da plataforma",
  OWNER: "Proprietário",
  MANAGER: "Administrador",
  MEMBER: "Colaborador",
};

export function getUserRoleLabel(role: UserRole) {
  return ROLE_LABELS[role];
}

export function isPlatformAdmin(role: UserRole | null | undefined) {
  return role === "SUPER_ADMIN";
}

export function isOwner(role: UserRole | null | undefined) {
  return role === "OWNER";
}

export function isCompanyManager(role: UserRole | null | undefined) {
  return role === "MANAGER";
}

export function isMember(role: UserRole | null | undefined) {
  return role === "MEMBER";
}

export function isTenantRole(role: UserRole | null | undefined) {
  return role === "OWNER" || role === "MANAGER" || role === "MEMBER";
}

export function canAccessPlatformAdmin(role: UserRole | null | undefined) {
  return isPlatformAdmin(role);
}

export function canAccessDashboard(role: UserRole | null | undefined) {
  return isTenantRole(role);
}

export function canViewCompany(role: UserRole | null | undefined) {
  return isTenantRole(role);
}

export function canEditCompany(role: UserRole | null | undefined) {
  return isOwner(role);
}

export function canManageBranding(role: UserRole | null | undefined) {
  return isOwner(role) || isCompanyManager(role);
}

export function canManageSettings(role: UserRole | null | undefined) {
  return isOwner(role);
}

export function canManageServices(role: UserRole | null | undefined) {
  return isOwner(role) || isCompanyManager(role);
}

export function canManageGallery(role: UserRole | null | undefined) {
  return isOwner(role) || isCompanyManager(role);
}

export function canAccessEstimates(role: UserRole | null | undefined) {
  return isTenantRole(role);
}

export function canAccessCustomers(role: UserRole | null | undefined) {
  return isTenantRole(role);
}

export function canManageTeam(role: UserRole | null | undefined) {
  return isOwner(role);
}
