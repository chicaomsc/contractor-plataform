import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { MeResponse, UserRole } from "@/features/auth/types/auth";
import DashboardBrandingPage from "./branding/page";
import DashboardCompanyPage from "./company/page";
import DashboardEstimatesPage from "./estimates/page";
import DashboardGalleryPage from "./gallery/page";
import DashboardServicesPage from "./services/page";
import DashboardSettingsPage from "./settings/page";
import DashboardTeamPage from "./team/page";

let role: UserRole = "OWNER";

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    session: tenantSession(role),
    isCheckingSession: false,
  }),
}));

vi.mock("@/features/dashboard/components/BrandingPage", () => ({
  BrandingPage: () => <div>Branding allowed</div>,
}));

vi.mock("@/features/dashboard/components/CompanyPage", () => ({
  CompanyPage: () => <div>Company allowed</div>,
}));

vi.mock("@/features/dashboard/components/estimates/EstimatesPage", () => ({
  EstimatesPage: () => <div>Estimates allowed</div>,
}));

vi.mock("@/features/dashboard/components/gallery/GalleryPage", () => ({
  GalleryPage: () => <div>Gallery allowed</div>,
}));

vi.mock("@/features/dashboard/components/services/ServicesPage", () => ({
  ServicesPage: () => <div>Services allowed</div>,
}));

vi.mock("@/features/dashboard/components/SettingsPage", () => ({
  SettingsPage: () => <div>Settings allowed</div>,
}));

vi.mock("@/features/dashboard/components/team/TeamPage", () => ({
  TeamPage: () => <div>Team allowed</div>,
}));

function tenantSession(nextRole: UserRole): MeResponse {
  return {
    user: {
      id: "user-1",
      companyId: nextRole === "SUPER_ADMIN" ? null : "company-1",
      email: "user@example.test",
      name: "User",
      role: nextRole,
      status: "ACTIVE",
    },
    company:
      nextRole === "SUPER_ADMIN"
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

function expectAccessDenied() {
  expect(
    screen.getByText("Você não tem permissão para acessar esta área."),
  ).toBeInTheDocument();
}

describe("dashboard direct route permissions", () => {
  beforeEach(() => {
    role = "OWNER";
  });

  it("denies MEMBER access to branding by direct URL", () => {
    role = "MEMBER";

    render(<DashboardBrandingPage />);

    expect(screen.queryByText("Branding allowed")).not.toBeInTheDocument();
    expectAccessDenied();
  });

  it("denies MEMBER access to services by direct URL", () => {
    role = "MEMBER";

    render(<DashboardServicesPage />);

    expect(screen.queryByText("Services allowed")).not.toBeInTheDocument();
    expectAccessDenied();
  });

  it("denies MEMBER access to gallery by direct URL", () => {
    role = "MEMBER";

    render(<DashboardGalleryPage />);

    expect(screen.queryByText("Gallery allowed")).not.toBeInTheDocument();
    expectAccessDenied();
  });

  it("denies MANAGER access to settings by direct URL", () => {
    role = "MANAGER";

    render(<DashboardSettingsPage />);

    expect(screen.queryByText("Settings allowed")).not.toBeInTheDocument();
    expectAccessDenied();
  });

  it("allows MANAGER access to branding by direct URL", () => {
    role = "MANAGER";

    render(<DashboardBrandingPage />);

    expect(screen.getByText("Branding allowed")).toBeInTheDocument();
  });

  it("allows MEMBER access to company by direct URL", () => {
    role = "MEMBER";

    render(<DashboardCompanyPage />);

    expect(screen.getByText("Company allowed")).toBeInTheDocument();
  });

  it("allows MEMBER access to estimates by direct URL", () => {
    role = "MEMBER";

    render(<DashboardEstimatesPage />);

    expect(screen.getByText("Estimates allowed")).toBeInTheDocument();
  });

  it("allows OWNER access to team by direct URL", () => {
    role = "OWNER";

    render(<DashboardTeamPage />);

    expect(screen.getByText("Team allowed")).toBeInTheDocument();
  });

  it.each(["MANAGER", "MEMBER"] as UserRole[])(
    "denies %s access to team by direct URL",
    (nextRole) => {
      role = nextRole;

      render(<DashboardTeamPage />);

      expect(screen.queryByText("Team allowed")).not.toBeInTheDocument();
      expectAccessDenied();
    },
  );
});
