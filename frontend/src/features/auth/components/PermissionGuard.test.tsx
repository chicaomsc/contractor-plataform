import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { canManageBranding } from "../permissions";
import type { MeResponse, UserRole } from "../types/auth";
import { PermissionGuard } from "./PermissionGuard";

let authState: {
  session: MeResponse | null;
  isCheckingSession: boolean;
};

vi.mock("../hooks/auth-context", () => ({
  useAuth: () => authState,
}));

function session(role: UserRole): MeResponse {
  return {
    user: {
      id: "user-1",
      companyId: role === "SUPER_ADMIN" ? null : "company-1",
      email: "user@example.test",
      name: "User",
      role,
      status: "ACTIVE",
    },
    company:
      role === "SUPER_ADMIN"
        ? null
        : {
            id: "company-1",
            name: "Company",
            slug: "company",
            email: null,
            country: "PT",
            status: "ACTIVE",
          },
    branding: null,
    settings: null,
  };
}

beforeEach(() => {
  authState = {
    session: session("OWNER"),
    isCheckingSession: false,
  };
});

describe("PermissionGuard", () => {
  it.each(["OWNER", "MANAGER"] as UserRole[])(
    "allows direct route access for %s when permission matches",
    (role) => {
      authState.session = session(role);

      render(
        <PermissionGuard canAccess={canManageBranding}>
          <div>Branding page</div>
        </PermissionGuard>,
      );

      expect(screen.getByText("Branding page")).toBeInTheDocument();
    },
  );

  it.each(["MEMBER", "SUPER_ADMIN"] as UserRole[])(
    "shows access denied for %s when permission does not match",
    (role) => {
      authState.session = session(role);

      render(
        <PermissionGuard canAccess={canManageBranding}>
          <div>Branding page</div>
        </PermissionGuard>,
      );

      expect(screen.queryByText("Branding page")).not.toBeInTheDocument();
      expect(
        screen.getByText("Você não tem permissão para acessar esta área."),
      ).toBeInTheDocument();
    },
  );

  it("shows a temporary loading state while session data is unavailable", () => {
    authState = {
      session: null,
      isCheckingSession: true,
    };

    render(
      <PermissionGuard canAccess={canManageBranding}>
        <div>Branding page</div>
      </PermissionGuard>,
    );

    expect(screen.getByText(/A validar permissões/)).toBeInTheDocument();
  });
});
