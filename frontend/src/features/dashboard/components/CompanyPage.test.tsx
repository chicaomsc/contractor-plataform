import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CompanyPage } from "./CompanyPage";
import type { UserRole } from "@/features/auth/types/auth";

const mutateAsync = vi.fn();
let role: UserRole = "OWNER";

const company = {
  id: "company-1",
  name: "JR Pinturas",
  tradeName: "JR Pinturas",
  slug: "jr-pinturas",
  email: "contato@example.com",
  phone: "999999",
  whatsapp: "888888",
  website: "https://example.test",
  taxNumber: "123",
  country: "PT",
  address: {
    street: "Rua Principal",
    city: "Lisboa",
    postalCode: "1000",
    region: "Lisboa",
    country: "PT",
  },
  status: "ACTIVE",
};

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
        country: "PT",
        status: "ACTIVE",
      },
      branding: null,
      settings: null,
    },
  }),
}));

vi.mock("../hooks/dashboard-hooks", () => ({
  useCompany: () => ({
    data: company,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
  useUpdateCompany: () => ({
    mutateAsync,
    isPending: false,
    isError: false,
    isSuccess: false,
  }),
}));

async function waitForCompanyPage() {
  await screen.findByRole("heading", { name: "Editar dados da empresa" });
}

describe("CompanyPage permissions", () => {
  beforeEach(() => {
    role = "OWNER";
    mutateAsync.mockReset();
  });

  it("keeps editing available for OWNER", async () => {
    role = "OWNER";

    render(<CompanyPage />);
    await waitForCompanyPage();

    expect(screen.getByRole("button", { name: "Guardar" })).toBeVisible();
    expect(screen.getByLabelText("Nome legal")).not.toBeDisabled();
  });

  it.each(["MANAGER", "MEMBER"] as UserRole[])(
    "shows company data read-only for %s",
    async (nextRole) => {
      role = nextRole;

      render(<CompanyPage />);
      await waitForCompanyPage();

      expect(screen.getByLabelText("Nome legal")).toHaveValue("JR Pinturas");
      expect(screen.getByLabelText("Nome comercial")).toHaveValue(
        "JR Pinturas",
      );
      expect(screen.queryByRole("button", { name: "Guardar" })).toBeNull();
      expect(screen.getByLabelText("Nome legal")).toBeDisabled();
      expect(screen.getByLabelText("Email")).toBeDisabled();

      fireEvent.submit(
        screen
          .getByRole("heading", { name: "Editar dados da empresa" })
          .closest("form")!,
      );

      await waitFor(() => expect(mutateAsync).not.toHaveBeenCalled());
    },
  );
});
