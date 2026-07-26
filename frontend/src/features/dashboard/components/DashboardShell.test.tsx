import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DashboardShell } from "./DashboardShell";

vi.mock("next/navigation", () => ({
  usePathname: () => "/dashboard/company",
}));

const logout = vi.fn();

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    logout,
    session: {
      user: {
        id: "user-1",
        companyId: "company-1",
        email: "owner@example.com",
        name: "Owner",
        role: "OWNER",
        status: "ACTIVE",
      },
      company: {
        id: "company-1",
        name: "JR Pinturas",
        slug: "jr-pinturas",
        email: "contato@example.com",
        country: "BR",
        status: "ACTIVE",
      },
      branding: null,
      settings: null,
    },
  }),
}));

describe("DashboardShell", () => {
  it("keeps the public site action out of the header", () => {
    render(
      <DashboardShell>
        <div>Conteúdo</div>
      </DashboardShell>,
    );

    expect(
      screen.queryByRole("link", { name: /visualizar site/i }),
    ).not.toBeInTheDocument();
  });
});
