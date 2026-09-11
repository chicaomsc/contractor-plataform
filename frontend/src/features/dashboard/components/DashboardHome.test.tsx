import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardHome } from "./DashboardHome";
import {
  useBranding,
  useCompany,
  useGallery,
  useOnboardingStatus,
  useServices,
  useSettings,
} from "../hooks/dashboard-hooks";
import type { UserRole } from "@/features/auth/types/auth";

let role: UserRole = "OWNER";

vi.mock("../hooks/dashboard-hooks", () => ({
  useBranding: vi.fn(),
  useCompany: vi.fn(),
  useGallery: vi.fn(),
  useOnboardingStatus: vi.fn(),
  useServices: vi.fn(),
  useSettings: vi.fn(),
}));

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    session: {
      user: {
        id: "user-1",
        companyId: "company-1",
        email: "user@example.test",
        name: "User",
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
    },
  }),
}));

function queryResult<T>(data: T) {
  return {
    data,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  };
}

function onboardingResult(
  data = {
    companyCompleted: false,
    brandingCompleted: false,
    servicesCompleted: false,
    customerCompleted: false,
    estimateCompleted: false,
    teamCompleted: false,
  },
) {
  return queryResult(data) as unknown as ReturnType<typeof useOnboardingStatus>;
}

describe("DashboardHome", () => {
  beforeEach(() => {
    role = "OWNER";
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    process.env.NEXT_PUBLIC_SITE_URL = "http://localhost:3001";
    process.env.NEXT_PUBLIC_PLATFORM_BASE_DOMAIN = "localhost";

    vi.mocked(useCompany).mockReturnValue(
      queryResult({
        id: "company-1",
        name: "JR Pinturas",
        tradeName: null,
        slug: "jr-pinturas",
        email: "contato@example.com",
        phone: null,
        whatsapp: null,
        website: null,
        taxNumber: null,
        country: "BR",
        address: null,
        status: "ACTIVE",
      }) as unknown as ReturnType<typeof useCompany>,
    );
    vi.mocked(useBranding).mockReturnValue(
      queryResult({
        id: "branding-1",
        companyId: "company-1",
        logoUrl: null,
        primaryColor: "#b43f08",
        secondaryColor: null,
        accentColor: null,
        tagline: null,
        aboutText: null,
        footerText: null,
        quotationPrefix: null,
        signatureName: null,
      }) as unknown as ReturnType<typeof useBranding>,
    );
    vi.mocked(useSettings).mockReturnValue(
      queryResult({
        id: "settings-1",
        companyId: "company-1",
        defaultCurrency: "BRL",
        defaultTaxRate: null,
        estimateValidityDays: null,
        estimateFooterText: null,
        locale: null,
        timezone: null,
        dateFormat: null,
        numberFormat: null,
      }) as unknown as ReturnType<typeof useSettings>,
    );
    vi.mocked(useServices).mockReturnValue(
      queryResult([]) as unknown as ReturnType<typeof useServices>,
    );
    vi.mocked(useGallery).mockReturnValue(
      queryResult([]) as unknown as ReturnType<typeof useGallery>,
    );
    vi.mocked(useOnboardingStatus).mockReturnValue(onboardingResult());
  });

  it("shows the public site link as the first next action", () => {
    render(<DashboardHome />);

    const actions = screen.getByText("Próximas ações").nextElementSibling;
    const links = within(actions as HTMLElement).getAllByRole("link");
    const publicSiteLink = links[0];

    expect(publicSiteLink).toHaveAccessibleName(/visualizar site/i);
    expect(publicSiteLink).toHaveAttribute(
      "href",
      "http://jr-pinturas.localhost:3001/",
    );
    expect(publicSiteLink).toHaveAttribute("target", "_blank");
    expect(publicSiteLink).toHaveAttribute("rel", "noopener noreferrer");
    expect(publicSiteLink).toHaveClass("bg-primary");
  });

  it("loads and renders onboarding only for OWNER", () => {
    render(<DashboardHome />);

    expect(useOnboardingStatus).toHaveBeenCalledWith({ enabled: true });
    expect(
      screen.getByRole("heading", { name: "Configure sua empresa" }),
    ).toBeInTheDocument();
    expect(screen.getByText("0 de 5")).toBeInTheDocument();
    expect(screen.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
  });

  it("does not request or render onboarding for tenant roles other than OWNER", () => {
    role = "MANAGER";

    render(<DashboardHome />);

    expect(useOnboardingStatus).toHaveBeenCalledWith({ enabled: false });
    expect(
      screen.queryByRole("heading", { name: "Configure sua empresa" }),
    ).not.toBeInTheDocument();

    role = "MEMBER";
    render(<DashboardHome />);

    expect(useOnboardingStatus).toHaveBeenLastCalledWith({ enabled: false });
    expect(
      screen.queryByRole("heading", { name: "Configure sua empresa" }),
    ).not.toBeInTheDocument();
  });

  it("calculates required progress without counting team", () => {
    vi.mocked(useOnboardingStatus).mockReturnValue(
      onboardingResult({
        companyCompleted: true,
        brandingCompleted: true,
        servicesCompleted: true,
        customerCompleted: false,
        estimateCompleted: false,
        teamCompleted: true,
      }),
    );

    render(<DashboardHome />);

    expect(screen.getByText("3 de 5")).toBeInTheDocument();
    expect(screen.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "60",
    );
    expect(screen.getByText("Convide sua equipe")).toBeInTheDocument();
    expect(screen.getByText("Opcional")).toBeInTheDocument();
  });

  it("shows completed state when all required steps are complete", () => {
    vi.mocked(useOnboardingStatus).mockReturnValue(
      onboardingResult({
        companyCompleted: true,
        brandingCompleted: true,
        servicesCompleted: true,
        customerCompleted: true,
        estimateCompleted: true,
        teamCompleted: false,
      }),
    );

    render(<DashboardHome />);

    expect(screen.getByText("5 de 5")).toBeInTheDocument();
    expect(screen.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "100",
    );
    expect(
      screen.getByText("Configuração essencial concluída."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Convidar colaboradores" }),
    ).toBeInTheDocument();
  });

  it("keeps the dashboard usable while onboarding is loading or fails", () => {
    vi.mocked(useOnboardingStatus).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useOnboardingStatus>);

    const { rerender } = render(<DashboardHome />);
    expect(
      screen.getByRole("heading", { name: "Visão geral" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Configure sua empresa" }),
    ).not.toBeInTheDocument();

    vi.mocked(useOnboardingStatus).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useOnboardingStatus>);
    rerender(<DashboardHome />);
    expect(
      screen.getByRole("heading", { name: "Visão geral" }),
    ).toBeInTheDocument();
  });

  it("uses the defined destinations for checklist actions", () => {
    render(<DashboardHome />);

    expect(
      screen.getByRole("link", { name: "Configurar Dados da empresa" }),
    ).toHaveAttribute("href", "/dashboard/company");
    expect(
      screen.getByRole("link", { name: "Configurar Identidade visual" }),
    ).toHaveAttribute("href", "/dashboard/branding");
    expect(
      screen.getByRole("link", { name: "Configurar Serviços" }),
    ).toHaveAttribute("href", "/dashboard/services");
    expect(
      screen.getByRole("link", { name: "Configurar Primeiro cliente" }),
    ).toHaveAttribute("href", "/dashboard/estimates/new");
    expect(
      screen.getByRole("link", { name: "Configurar Primeiro orçamento" }),
    ).toHaveAttribute("href", "/dashboard/estimates/new");
    expect(
      screen.getByRole("link", { name: "Convidar colaboradores" }),
    ).toHaveAttribute("href", "/dashboard/team");
  });

  it("does not request OWNER-only settings for MANAGER", () => {
    role = "MANAGER";

    render(<DashboardHome />);

    expect(useBranding).toHaveBeenCalledWith({ enabled: true });
    expect(useServices).toHaveBeenCalledWith({ enabled: true });
    expect(useGallery).toHaveBeenCalledWith({ enabled: true });
    expect(useSettings).toHaveBeenCalledWith({ enabled: false });
    expect(screen.queryByRole("link", { name: "Ajustar settings" })).toBeNull();
  });

  it("does not request restricted dashboard resources for MEMBER", () => {
    role = "MEMBER";

    render(<DashboardHome />);

    expect(useBranding).toHaveBeenCalledWith({ enabled: false });
    expect(useSettings).toHaveBeenCalledWith({ enabled: false });
    expect(useServices).toHaveBeenCalledWith({ enabled: false });
    expect(useGallery).toHaveBeenCalledWith({ enabled: false });
    expect(screen.getByRole("link", { name: "Editar empresa" })).toBeVisible();
    expect(
      screen.queryByRole("link", { name: "Rever branding" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Gerenciar serviços" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Gerenciar galeria" }),
    ).not.toBeInTheDocument();
  });
});
