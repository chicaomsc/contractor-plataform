import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CompanyDetailPage } from "./CompanyDetailPage";
import {
  generateOwnerPasswordResetLink,
  getCompany,
} from "../api/platform-admin-api";
import { ApiError } from "@/lib/api/errors";

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    accessToken: "access-token",
  }),
}));

vi.mock("../api/platform-admin-api", () => ({
  getCompany: vi.fn(),
  generateOwnerPasswordResetLink: vi.fn(),
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
    vi.restoreAllMocks();
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
    vi.mocked(generateOwnerPasswordResetLink).mockResolvedValue({
      resetLink: "http://localhost:3001/reset-password#token=reset-token",
      expiresAt: "2026-08-01T12:30:00Z",
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

  it("shows password reset action for ACTIVE owners and copies the generated link", async () => {
    const user = userEvent.setup();
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
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
      owners: [
        {
          id: "owner-1",
          email: "owner@tenant.test",
          name: "Owner Active",
          role: "OWNER",
          status: "ACTIVE",
          createdAt: "2026-07-26T10:00:00Z",
        },
      ],
    });

    renderWithQueryClient();

    await user.click(
      await screen.findByRole("button", {
        name: /gerar link de recuperação/i,
      }),
    );

    expect(generateOwnerPasswordResetLink).toHaveBeenCalledWith(
      "access-token",
      "company-1",
      "owner-1",
    );
    expect(await screen.findByLabelText("Link de recuperação")).toHaveValue(
      "http://localhost:3001/reset-password#token=reset-token",
    );

    await user.click(screen.getByRole("button", { name: /copiar link/i }));

    expect(writeText).toHaveBeenCalledWith(
      "http://localhost:3001/reset-password#token=reset-token",
    );
  });

  it("does not show password reset action for PENDING owners", async () => {
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
      owners: [
        {
          id: "owner-1",
          email: "owner@tenant.test",
          name: "Owner Pending",
          role: "OWNER",
          status: "PENDING",
          createdAt: "2026-07-26T10:00:00Z",
        },
      ],
    });

    renderWithQueryClient();

    expect(await screen.findByText("Owner Pending")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /gerar link de recuperação/i }),
    ).not.toBeInTheDocument();
  });

  it("handles password reset conflicts for owners that are no longer eligible", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(generateOwnerPasswordResetLink).mockRejectedValue(
      new ApiError("Owner não está ACTIVE.", 409),
    );
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
      owners: [
        {
          id: "owner-1",
          email: "owner@tenant.test",
          name: "Owner Active",
          role: "OWNER",
          status: "ACTIVE",
          createdAt: "2026-07-26T10:00:00Z",
        },
      ],
    });

    renderWithQueryClient();

    await user.click(
      await screen.findByRole("button", {
        name: /gerar link de recuperação/i,
      }),
    );

    expect(
      await screen.findByText("A operação já não é válida para o estado atual."),
    ).toBeInTheDocument();
  });
});
