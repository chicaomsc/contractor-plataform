import { expect, test, type Page } from "@playwright/test";
import { loginViaUi, registerAccount } from "./helpers";

async function saveBranding(page: Page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith("/branding/me") &&
      response.request().method() === "PUT",
  );

  await page.locator("form").getByRole("button", { name: "Guardar" }).click();
  const response = await responsePromise;
  expect(response.ok()).toBe(true);
}

async function openBranding(page: Page) {
  await page.goto("/dashboard/branding");
  await expect(
    page.getByRole("heading", { name: "Editar identidade visual" }),
  ).toBeVisible();
}

function colorInput(page: Page, name: string) {
  return page.getByRole("textbox", { name, exact: true });
}

test("aplica paleta, atualiza preview e persiste após guardar", async ({
  page,
  request,
}) => {
  const suffix = `branding.palette.${Date.now()}`;
  const { email, password } = await registerAccount(request, suffix);

  await loginViaUi(page, email, password);
  await openBranding(page);
  await page.getByRole("button", { name: /Moderna/ }).click();

  await expect(colorInput(page, "Cor primária")).toHaveValue("#0F766E");
  await expect(colorInput(page, "Cor secundária")).toHaveValue("#14B8A6");
  await expect(colorInput(page, "Cor de acento")).toHaveValue("#F97316");
  await expect(page.getByLabel("Amostra auxiliar #14B8A6")).toBeVisible();

  await saveBranding(page);
  await page.reload();

  await expect(colorInput(page, "Cor primária")).toHaveValue("#0F766E");
  await expect(colorInput(page, "Cor secundária")).toHaveValue("#14B8A6");
  await expect(colorInput(page, "Cor de acento")).toHaveValue("#F97316");
});

test("edita HEX manualmente, normaliza e persiste após guardar", async ({
  page,
  request,
}) => {
  const suffix = `branding.hex.${Date.now()}`;
  const { email, password } = await registerAccount(request, suffix);

  await loginViaUi(page, email, password);
  await openBranding(page);
  await colorInput(page, "Cor primária").fill("#be123c");
  await page.getByRole("textbox", { name: "Tagline" }).focus();

  await expect(colorInput(page, "Cor primária")).toHaveValue("#BE123C");
  await expect(page.getByLabel("Amostra principal #BE123C")).toBeVisible();

  await saveBranding(page);
  await page.reload();

  await expect(colorInput(page, "Cor primária")).toHaveValue("#BE123C");
});

test("permite experimentar cores sem persistir automaticamente", async ({
  page,
  request,
}) => {
  const suffix = `branding.unsaved.${Date.now()}`;
  const { email, password } = await registerAccount(request, suffix);

  await loginViaUi(page, email, password);
  await openBranding(page);
  await colorInput(page, "Cor primária").fill("#374151");
  await colorInput(page, "Cor de acento").fill("#B45309");
  await saveBranding(page);

  await page.getByRole("button", { name: /Vibrante/ }).click();
  await expect(colorInput(page, "Cor primária")).toHaveValue("#BE123C");
  await page.reload();

  await expect(colorInput(page, "Cor primária")).toHaveValue("#374151");
  await expect(colorInput(page, "Cor de acento")).toHaveValue("#B45309");
});

test("suporta abertura e seleção do picker por teclado", async ({
  page,
  request,
}) => {
  const suffix = `branding.keyboard.${Date.now()}`;
  const { email, password } = await registerAccount(request, suffix);

  await loginViaUi(page, email, password);
  await openBranding(page);

  const trigger = page.getByRole("button", {
    name: "Abrir seletor visual da cor principal",
    exact: true,
  });
  await trigger.focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("dialog", { name: "Selecionar cor primária" }),
  ).toBeVisible();

  const swatch = page.getByRole("button", { name: "Selecionar #2563EB" });
  await swatch.focus();
  await page.keyboard.press("Enter");
  await expect(colorInput(page, "Cor primária")).toHaveValue("#2563EB");

  await page.keyboard.press("Escape");
  await expect(
    page.getByRole("dialog", { name: "Selecionar cor primária" }),
  ).toHaveCount(0);
  await expect(trigger).toBeFocused();
});
