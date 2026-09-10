import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { MeResponse, UserRole } from "@/features/auth/types/auth";
import { DashboardShell } from "./DashboardShell";

let pathname = "/dashboard/company";
let session: MeResponse;
const logout = vi.fn();

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
}));

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    logout,
    session,
  }),
}));

function tenantSession(role: UserRole): MeResponse {
  return {
    user: {
      id: "user-1",
      companyId: "company-1",
      email: "owner@example.com",
      name: "Owner",
      role,
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
  };
}

function renderShell(role: UserRole) {
  session = tenantSession(role);
  return render(
    <DashboardShell>
      <div>Conteúdo</div>
    </DashboardShell>,
  );
}

function expectVisibleLinks(labels: string[]) {
  for (const label of labels) {
    expect(screen.getAllByRole("link", { name: label }).length).toBeGreaterThan(
      0,
    );
  }
}

function expectHiddenLinks(labels: string[]) {
  for (const label of labels) {
    expect(screen.queryByRole("link", { name: label })).not.toBeInTheDocument();
  }
}

describe("DashboardShell", () => {
  beforeEach(() => {
    pathname = "/dashboard/company";
    logout.mockClear();
  });

  it("keeps the public site action out of the header", () => {
    renderShell("OWNER");

    expect(
      screen.queryByRole("link", { name: /visualizar site/i }),
    ).not.toBeInTheDocument();
  });

  it("shows all tenant dashboard navigation items for OWNER", () => {
    renderShell("OWNER");

    expectVisibleLinks([
      "Início",
      "Empresa",
      "Branding",
      "Settings",
      "Serviços",
      "Galeria",
      "Orçamentos",
      "Equipe",
    ]);
  });

  it("hides OWNER-only settings from MANAGER navigation", () => {
    renderShell("MANAGER");

    expectVisibleLinks([
      "Início",
      "Empresa",
      "Branding",
      "Serviços",
      "Galeria",
      "Orçamentos",
    ]);
    expectHiddenLinks(["Settings"]);
    expectHiddenLinks(["Equipe"]);
  });

  it("shows only allowed areas for MEMBER navigation", () => {
    renderShell("MEMBER");

    expectVisibleLinks(["Início", "Empresa", "Orçamentos"]);
    expectHiddenLinks(["Branding", "Settings", "Serviços", "Galeria", "Equipe"]);
  });

  it("uses the same navigation filtering in the mobile sidebar", async () => {
    const user = userEvent.setup();
    renderShell("MEMBER");

    await user.click(screen.getByRole("button", { name: "Abrir menu" }));

    const mobileNav = screen.getAllByLabelText("Navegação do dashboard").at(1);
    expect(mobileNav).toBeTruthy();
    expect(mobileNav).toHaveTextContent("Início");
    expect(mobileNav).toHaveTextContent("Empresa");
    expect(mobileNav).toHaveTextContent("Orçamentos");
    expect(mobileNav).not.toHaveTextContent("Branding");
    expect(mobileNav).not.toHaveTextContent("Settings");
    expect(mobileNav).not.toHaveTextContent("Serviços");
    expect(mobileNav).not.toHaveTextContent("Galeria");
    expect(mobileNav).not.toHaveTextContent("Equipe");
  });

  it("shows the friendly role label in the user menu", () => {
    renderShell("MANAGER");

    expect(screen.getByText("Administrador")).toBeInTheDocument();
    expect(screen.queryByText("MANAGER")).not.toBeInTheDocument();
  });
});
