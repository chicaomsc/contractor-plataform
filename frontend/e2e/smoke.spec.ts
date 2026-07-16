import { expect, test } from "@playwright/test";
import { apiBaseUrl, loginViaUi, registerAccount } from "./helpers";

test("landing pública responde sem erro de aplicação", async ({ page }) => {
  const response = await page.goto("/");

  expect(response?.status()).toBeLessThan(500);
  await expect(page.getByRole("banner")).toBeVisible();
  await expect(page.getByText("Application error")).toHaveCount(0);
});

test("login inválido mostra erro útil", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill("invalid@example.test");
  await page.getByLabel("Password").fill("wrong-password");
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page.getByText("Sessão expirada")).toBeVisible();
});

test("dashboard sem sessão redireciona para login", async ({ page }) => {
  await page.goto("/dashboard");

  await expect(page).toHaveURL(/\/login\?next=%2Fdashboard/);
});

test("tenant inexistente retorna 404 no endpoint público", async ({
  request,
}) => {
  const response = await request.get(
    `${apiBaseUrl}/public/sites/tenant-inexistente-e2e`,
  );

  expect(response.status()).toBe(404);
});

test("logout encerra a sessão e bloqueia rota administrativa", async ({
  page,
  request,
}) => {
  const suffix = `logout.${Date.now()}`;
  const { email, password } = await registerAccount(request, suffix);

  await loginViaUi(page, email, password);
  let logoutButton = page.getByRole("button", { name: /Sair/ }).filter({
    visible: true,
  });
  if ((await logoutButton.count()) === 0) {
    await page.getByRole("button", { name: "Abrir menu" }).click();
    logoutButton = page.getByRole("button", { name: /Sair/ }).filter({
      visible: true,
    });
  }
  await logoutButton.first().click();

  await expect(page).toHaveURL(/\/login/);
  await page.goto("/dashboard/company");
  await expect(page).toHaveURL(/\/login\?next=%2Fdashboard%2Fcompany/);
});
