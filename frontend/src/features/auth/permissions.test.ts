import { describe, expect, it } from "vitest";
import {
  canAccessCustomers,
  canAccessDashboard,
  canAccessEstimates,
  canAccessPlatformAdmin,
  canEditCompany,
  canManageBranding,
  canManageGallery,
  canManageServices,
  canManageSettings,
  canManageTeam,
  canViewCompany,
  getUserRoleLabel,
  isCompanyManager,
  isMember,
  isOwner,
  isPlatformAdmin,
  isTenantRole,
} from "./permissions";
import type { UserRole } from "./types/auth";

const roles: UserRole[] = ["SUPER_ADMIN", "OWNER", "MANAGER", "MEMBER"];

describe("permissions", () => {
  it("centralizes user role labels", () => {
    expect(getUserRoleLabel("OWNER")).toBe("Proprietário");
    expect(getUserRoleLabel("MANAGER")).toBe("Administrador");
    expect(getUserRoleLabel("MEMBER")).toBe("Colaborador");
    expect(getUserRoleLabel("SUPER_ADMIN")).toBe("Administrador da plataforma");
  });

  it("identifies roles explicitly without generic admin semantics", () => {
    expect(isPlatformAdmin("SUPER_ADMIN")).toBe(true);
    expect(isOwner("OWNER")).toBe(true);
    expect(isCompanyManager("MANAGER")).toBe(true);
    expect(isMember("MEMBER")).toBe(true);
    expect(isTenantRole("OWNER")).toBe(true);
    expect(isTenantRole("MANAGER")).toBe(true);
    expect(isTenantRole("MEMBER")).toBe(true);
    expect(isTenantRole("SUPER_ADMIN")).toBe(false);
  });

  it("enforces the tenant permissions matrix", () => {
    const matrix = roles.map((role) => ({
      role,
      dashboard: canAccessDashboard(role),
      companyView: canViewCompany(role),
      companyEdit: canEditCompany(role),
      branding: canManageBranding(role),
      settings: canManageSettings(role),
      services: canManageServices(role),
      gallery: canManageGallery(role),
      estimates: canAccessEstimates(role),
      customers: canAccessCustomers(role),
      team: canManageTeam(role),
      platformAdmin: canAccessPlatformAdmin(role),
    }));

    expect(matrix).toEqual([
      {
        role: "SUPER_ADMIN",
        dashboard: false,
        companyView: false,
        companyEdit: false,
        branding: false,
        settings: false,
        services: false,
        gallery: false,
        estimates: false,
        customers: false,
        team: false,
        platformAdmin: true,
      },
      {
        role: "OWNER",
        dashboard: true,
        companyView: true,
        companyEdit: true,
        branding: true,
        settings: true,
        services: true,
        gallery: true,
        estimates: true,
        customers: true,
        team: true,
        platformAdmin: false,
      },
      {
        role: "MANAGER",
        dashboard: true,
        companyView: true,
        companyEdit: false,
        branding: true,
        settings: false,
        services: true,
        gallery: true,
        estimates: true,
        customers: true,
        team: false,
        platformAdmin: false,
      },
      {
        role: "MEMBER",
        dashboard: true,
        companyView: true,
        companyEdit: false,
        branding: false,
        settings: false,
        services: false,
        gallery: false,
        estimates: true,
        customers: true,
        team: false,
        platformAdmin: false,
      },
    ]);
  });
});
