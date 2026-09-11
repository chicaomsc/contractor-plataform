import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthGuard, AdminGuard } from "./AuthGuard";
import type { MeResponse, UserRole } from "../types/auth";

const replaceMock = vi.fn();
let pathname = "/dashboard";
let authState: {
  accessToken: string | null;
  isAuthenticated: boolean;
  isCheckingSession: boolean;
  isSessionExpired: boolean;
  session: MeResponse | null;
  logout: ReturnType<typeof vi.fn>;
};

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ replace: replaceMock }),
}));

vi.mock("../hooks/auth-context", () => ({
  useAuth: () => authState,
}));

function tenantSession(role: UserRole): MeResponse {
  return {
    user: {
      id: `${role.toLowerCase()}-user`,
      companyId: "company-1",
      email: `${role.toLowerCase()}@example.test`,
      name: role,
      role,
      status: "ACTIVE",
    },
    company: {
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

function platformSession(): MeResponse {
  return {
    user: {
      id: "platform-user",
      companyId: null,
      email: "platform@example.test",
      name: "Platform",
      role: "SUPER_ADMIN",
      status: "ACTIVE",
    },
    company: null,
    branding: null,
    settings: null,
  };
}

function setAuthenticatedSession(session: MeResponse) {
  authState = {
    accessToken: "access-token",
    isAuthenticated: true,
    isCheckingSession: false,
    isSessionExpired: false,
    session,
    logout: vi.fn(),
  };
}

beforeEach(() => {
  pathname = "/dashboard";
  replaceMock.mockClear();
  setAuthenticatedSession(tenantSession("OWNER"));
});

describe("AuthGuard", () => {
  it.each(["OWNER", "MANAGER", "MEMBER"] as UserRole[])(
    "allows %s into the tenant dashboard",
    (role) => {
      setAuthenticatedSession(tenantSession(role));

      render(
        <AuthGuard>
          <div>Dashboard content</div>
        </AuthGuard>,
      );

      expect(screen.getByText("Dashboard content")).toBeInTheDocument();
      expect(screen.queryByText("A validar sessão")).not.toBeInTheDocument();
      expect(replaceMock).not.toHaveBeenCalled();
    },
  );

  it("keeps SUPER_ADMIN out of the tenant dashboard", () => {
    setAuthenticatedSession(platformSession());

    render(
      <AuthGuard>
        <div>Dashboard content</div>
      </AuthGuard>,
    );

    expect(screen.queryByText("Dashboard content")).not.toBeInTheDocument();
    expect(screen.getByText(/A validar sessão/)).toBeInTheDocument();
    expect(replaceMock).toHaveBeenCalledWith("/admin");
  });

  it("redirects unauthenticated users to login", () => {
    authState = {
      accessToken: null,
      isAuthenticated: false,
      isCheckingSession: false,
      isSessionExpired: false,
      session: null,
      logout: vi.fn(),
    };
    pathname = "/dashboard/settings";

    render(
      <AuthGuard>
        <div>Dashboard content</div>
      </AuthGuard>,
    );

    expect(replaceMock).toHaveBeenCalledWith(
      "/login?next=%2Fdashboard%2Fsettings",
    );
    expect(screen.getByText(/A validar sessão/)).toBeInTheDocument();
  });

  it("shows loading only while the session is actually being checked", () => {
    authState = {
      accessToken: "access-token",
      isAuthenticated: false,
      isCheckingSession: true,
      isSessionExpired: false,
      session: null,
      logout: vi.fn(),
    };

    render(
      <AuthGuard>
        <div>Dashboard content</div>
      </AuthGuard>,
    );

    expect(screen.getByText(/A validar sessão/)).toBeInTheDocument();
    expect(screen.queryByText("Dashboard content")).not.toBeInTheDocument();
  });
});

describe("AdminGuard", () => {
  it("allows SUPER_ADMIN into platform admin", () => {
    pathname = "/admin";
    setAuthenticatedSession(platformSession());

    render(
      <AdminGuard>
        <div>Platform content</div>
      </AdminGuard>,
    );

    expect(screen.getByText("Platform content")).toBeInTheDocument();
  });

  it.each(["OWNER", "MANAGER", "MEMBER"] as UserRole[])(
    "keeps %s out of platform admin",
    (role) => {
      pathname = "/admin";
      setAuthenticatedSession(tenantSession(role));

      render(
        <AdminGuard>
          <div>Platform content</div>
        </AdminGuard>,
      );

      expect(screen.queryByText("Platform content")).not.toBeInTheDocument();
      expect(replaceMock).toHaveBeenCalledWith("/dashboard");
    },
  );
});
