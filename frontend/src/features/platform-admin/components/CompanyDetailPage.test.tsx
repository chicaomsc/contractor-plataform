import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CompanyDetailPage } from "./CompanyDetailPage";
import { getCompany } from "../api/platform-admin-api";

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    accessToken: "access-token",
  }),
}));

vi.mock("../api/platform-admin-api", () => ({
  getCompany: vi.fn(),
  inviteOwner: vi.fn(),
  reissueInvite: vi.fn(),
  revokeInvite: vi.fn(),
  updateCompanyStatus: vi.fn(),
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <CompanyDetailPage companyId="company-1" />
    </QueryClientProvider>,
  );
}

describe("CompanyDetailPage", () => {
  beforeEach(() => {
    vi.mocked(getCompany).mockResolvedValue({
      company: {
        id: "company-1",
        name: "Tenant A",
        slug: "tenant-a",
        email: "contato@tenant.test",
        country: "BR",
        tradeName: "Tenant A Serviços",
        status: "ACTIVE",
        createdAt: "2026-07-26T10:00:00Z",
      },
      owners: [],
    });
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    process.env.NEXT_PUBLIC_SITE_URL = "http://localhost:3001";
    process.env.NEXT_PUBLIC_PLATFORM_BASE_DOMAIN = "localhost";
  });

  it("links to the selected company public landing", async () => {
    renderWithQueryClient();

    const link = await screen.findByRole("link", {
      name: /visualizar landing/i,
    });

    expect(link).toHaveAttribute("href", "http://tenant-a.localhost:3001/");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });
});
