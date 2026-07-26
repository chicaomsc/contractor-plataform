import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardHome } from "./DashboardHome";
import {
  useBranding,
  useCompany,
  useGallery,
  useServices,
  useSettings,
} from "../hooks/dashboard-hooks";

vi.mock("../hooks/dashboard-hooks", () => ({
  useBranding: vi.fn(),
  useCompany: vi.fn(),
  useGallery: vi.fn(),
  useServices: vi.fn(),
  useSettings: vi.fn(),
}));

function queryResult<T>(data: T) {
  return {
    data,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  };
}

describe("DashboardHome", () => {
  beforeEach(() => {
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
});
