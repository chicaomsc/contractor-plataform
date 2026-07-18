import { expect, test } from "@playwright/test";
import { apiBaseUrl, loginViaUi, registerAccount } from "./helpers";

async function createEstimateWithPdfData(
  request: import("@playwright/test").APIRequestContext,
  accessToken: string,
) {
  const customerResponse = await request.post(`${apiBaseUrl}/customers`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { name: "E2E PDF Customer", email: "e2e.pdf.customer@example.test" },
  });
  expect(customerResponse.status()).toBe(201);
  const customer = (await customerResponse.json()) as { id: string };

  const estimateResponse = await request.post(`${apiBaseUrl}/estimates`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: {
      customerId: customer.id,
      title: "E2E PDF Estimate",
      items: [
        { description: "Serviço E2E", quantity: 1, unit: "UNIT", unitPrice: 100 },
      ],
      materials: [],
    },
  });
  expect(estimateResponse.status()).toBe(201);
  return (await estimateResponse.json()) as { id: string; number: string };
}

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

test("baixar PDF de um orçamento faz download de um ficheiro não vazio", async ({
  page,
  request,
}) => {
  const suffix = `pdf.${Date.now()}`;
  const { auth, email, password } = await registerAccount(request, suffix);

  const estimate = await createEstimateWithPdfData(request, auth.accessToken);

  await loginViaUi(page, email, password);
  await page.goto(`/dashboard/estimates/${estimate.id}`);
  await expect(page.getByText(estimate.number)).toBeVisible();

  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: /Baixar PDF/ }).click();
  const download = await downloadPromise;

  expect(download.suggestedFilename()).toMatch(/^orcamento-.*\.pdf$/);
  const downloadPath = await download.path();
  expect(downloadPath).toBeTruthy();
  const fs = await import("node:fs");
  const stats = fs.statSync(downloadPath as string);
  expect(stats.size).toBeGreaterThan(0);
});

test("partilhar um orçamento gera um link público, funcional sem sessão, com download de PDF", async ({
  page,
  browser,
  request,
}) => {
  const suffix = `share.${Date.now()}`;
  const { auth, email, password } = await registerAccount(request, suffix);
  const estimate = await createEstimateWithPdfData(request, auth.accessToken);

  await loginViaUi(page, email, password);
  await page.goto(`/dashboard/estimates/${estimate.id}`);
  await expect(page.getByText(estimate.number)).toBeVisible();

  await page.getByRole("button", { name: "Compartilhar" }).click();
  const linkInput = page.getByLabel("Link público do orçamento");
  await expect(linkInput).toBeVisible();
  const shareUrl = await linkInput.inputValue();
  expect(shareUrl).toContain("/share/");
  expect(shareUrl).not.toContain(`/estimate/${estimate.id}`);

  const whatsappLink = page.getByRole("link", { name: /WhatsApp/ });
  await expect(whatsappLink).toHaveAttribute("href", /^https:\/\/wa\.me\/\?text=/);

  // Open the link in a brand-new, cookie-less browser context — proves it needs no session.
  const publicContext = await browser.newContext();
  const publicPage = await publicContext.newPage();
  await publicPage.goto(shareUrl);
  await expect(publicPage.getByText(estimate.number)).toBeVisible();
  await expect(publicPage.getByText("E2E PDF Customer")).toBeVisible();

  const downloadPromise = publicPage.waitForEvent("download");
  await publicPage.getByRole("link", { name: /Baixar PDF/ }).click();
  const download = await downloadPromise;
  const downloadPath = await download.path();
  expect(downloadPath).toBeTruthy();
  const fs = await import("node:fs");
  expect(fs.statSync(downloadPath as string).size).toBeGreaterThan(0);

  // Revoking invalidates the link immediately, even for a page that already loaded it.
  await page.getByRole("button", { name: "Revogar" }).click();
  await publicPage.reload();
  await expect(publicPage.getByText("Link indisponível")).toBeVisible();

  await publicContext.close();
});
