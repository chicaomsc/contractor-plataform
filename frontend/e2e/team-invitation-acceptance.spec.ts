import { expect, test, type Page } from "@playwright/test";

function authResponse(role: "MANAGER" | "MEMBER", name: string) {
  return {
    accessToken: `access-${role.toLowerCase()}`,
    refreshToken: `refresh-${role.toLowerCase()}`,
    user: {
      id: `${role.toLowerCase()}-user`,
      companyId: "company-1",
      email: `${role.toLowerCase()}@example.test`,
      name,
      role,
      status: "ACTIVE",
    },
    company: {
      id: "company-1",
      name: "JR Pinturas",
      slug: "jr-pinturas",
      email: null,
      country: "PT",
      status: "ACTIVE",
    },
  };
}

async function mockDashboardFor(page: Page, role: "MANAGER" | "MEMBER", name: string) {
  const auth = authResponse(role, name);
  await page.route("**/api/auth/me", (route) =>
    route.fulfill({ json: { user: auth.user, company: auth.company, branding: null, settings: null } }),
  );
  await page.route("**/api/company/me", (route) =>
    route.fulfill({
      json: {
        id: "company-1",
        name: "JR Pinturas",
        tradeName: "JR Pinturas",
        slug: "jr-pinturas",
        email: "contato@example.test",
        phone: null,
        whatsapp: null,
        website: null,
        taxNumber: null,
        country: "PT",
        address: null,
        status: "ACTIVE",
      },
    }),
  );
  await page.route("**/api/branding/me", (route) =>
    route.fulfill({
      json: {
        id: "branding-1",
        companyId: "company-1",
        logoUrl: null,
        primaryColor: "#1E40AF",
        secondaryColor: "#3B82F6",
        accentColor: "#F59E0B",
        tagline: null,
        aboutText: null,
        footerText: null,
        quotationPrefix: null,
        signatureName: null,
      },
    }),
  );
  await page.route("**/api/services", (route) => route.fulfill({ json: [] }));
  await page.route("**/api/gallery", (route) => route.fulfill({ json: [] }));
}

async function fillAcceptForm(page: Page, name: string) {
  await page.getByLabel("Nome").fill(name);
  await page.getByRole("textbox", { name: "Senha", exact: true }).fill("Password123");
  await page.getByRole("textbox", { name: "Confirmar senha" }).fill("Password123");
  await page.getByRole("button", { name: "Criar acesso" }).click();
}

test("accepts a MANAGER invitation and applies manager permissions", async ({ page }) => {
  await mockDashboardFor(page, "MANAGER", "Maria Manager");
  await page.route("**/api/auth/team-invitations/accept", async (route) => {
    const body = route.request().postDataJSON();
    expect(body).toEqual({
      token: "manager-token",
      name: "Maria Manager",
      password: "Password123",
    });
    await route.fulfill({ json: authResponse("MANAGER", "Maria Manager") });
  });

  await page.goto("/invite/team#token=manager-token");
  await expect(page).toHaveURL(/\/invite\/team$/);
  await expect(page.locator("body")).not.toContainText("manager-token");

  await fillAcceptForm(page, "Maria Manager");

  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText("Administrador")).toBeVisible();
  const sidebar = page.getByTestId("dashboard-sidebar");
  await expect(sidebar.getByRole("link", { name: "Branding" })).toBeVisible();
  await expect(sidebar.getByRole("link", { name: "Serviços" })).toBeVisible();
  await expect(sidebar.getByRole("link", { name: "Galeria" })).toBeVisible();
  await expect(sidebar.getByRole("link", { name: "Settings" })).toHaveCount(0);
  await expect(sidebar.getByRole("link", { name: "Equipe" })).toHaveCount(0);
});

test("accepts a MEMBER invitation and applies member permissions", async ({ page }) => {
  await mockDashboardFor(page, "MEMBER", "João Member");
  await page.route("**/api/auth/team-invitations/accept", (route) =>
    route.fulfill({ json: authResponse("MEMBER", "João Member") }),
  );

  await page.goto("/invite/team#token=member-token");
  await expect(page).toHaveURL(/\/invite\/team$/);
  await fillAcceptForm(page, "João Member");

  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText("Colaborador")).toBeVisible();
  const sidebar = page.getByTestId("dashboard-sidebar");
  await expect(sidebar.getByRole("link", { name: "Empresa" })).toBeVisible();
  await expect(sidebar.getByRole("link", { name: "Orçamentos" })).toBeVisible();
  await expect(sidebar.getByRole("link", { name: "Branding" })).toHaveCount(0);
  await expect(sidebar.getByRole("link", { name: "Serviços" })).toHaveCount(0);
  await expect(sidebar.getByRole("link", { name: "Galeria" })).toHaveCount(0);
  await expect(sidebar.getByRole("link", { name: "Settings" })).toHaveCount(0);
  await expect(sidebar.getByRole("link", { name: "Equipe" })).toHaveCount(0);
});

test("reusing the same invitation token shows the generic public error", async ({ page }) => {
  await mockDashboardFor(page, "MEMBER", "First User");
  let attempts = 0;
  await page.route("**/api/auth/team-invitations/accept", async (route) => {
    attempts += 1;
    if (attempts === 1) {
      await route.fulfill({ json: authResponse("MEMBER", "First User") });
      return;
    }

    await route.fulfill({
      status: 422,
      json: { detail: "O convite é inválido ou não está mais disponível." },
    });
  });

  await page.goto("/invite/team#token=one-time-token");
  await fillAcceptForm(page, "First User");
  await expect(page).toHaveURL(/\/dashboard/);

  await page.goto("/invite/team#token=one-time-token");
  await expect(page).toHaveURL(/\/invite\/team$/);
  await fillAcceptForm(page, "Second User");

  await expect(
    page.getByText("Este convite é inválido ou não está mais disponível."),
  ).toBeVisible();
  expect(attempts).toBe(2);
});
