import path from "node:path";
import { expect, test, type Page } from "@playwright/test";
import {
  apiBaseUrl,
  deleteCreatedGalleryItems,
  deleteCreatedServices,
  loginViaUi,
  registerAccount,
} from "./helpers";

async function saveFormAndWaitForPut(page: Page, path: string) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith(path) && response.request().method() === "PUT",
  );

  await page.locator("form").getByRole("button", { name: "Guardar" }).click();
  const response = await responsePromise;
  expect(response.ok()).toBe(true);
}

test("fluxo principal do MVP reflete dados administrativos no público", async ({
  page,
  request,
}) => {
  test.setTimeout(120_000);
  const suffix = Date.now().toString();
  const serviceName = `Pintura E2E ${suffix}`;
  const secondServiceName = `Acabamento E2E ${suffix}`;
  const editedServiceName = `Pintura E2E Editada ${suffix}`;
  const galleryTitle = `Galeria E2E ${suffix}`;
  const imagePath = path.resolve(
    process.cwd(),
    "../assets/tenants/jr-pinturas/logo/jr-logo.png",
  );
  const { auth, email, password } = await registerAccount(request, suffix);

  try {
    await loginViaUi(page, email, password);

    await page.goto("/dashboard/company");
    await page.getByLabel("Nome comercial").fill(`E2E Pinturas ${suffix}`);
    await page.getByLabel("Telefone").fill("+351 210 000 001");
    await page.getByLabel("WhatsApp").fill("+351 910 000 001");
    await page.getByLabel("Cidade").fill("Lisboa");
    await saveFormAndWaitForPut(page, "/company/me");

    await page.goto("/dashboard/branding");
    await page.getByLabel("Cor primária").fill("#0F766E");
    await page.getByLabel("Cor secundária").fill("#2563EB");
    await page.getByLabel("Cor de acento").fill("#F59E0B");
    await page.getByLabel("Tagline").fill(`Tagline E2E ${suffix}`);
    await saveFormAndWaitForPut(page, "/branding/me");

    await page.goto("/dashboard/settings");
    await page.getByLabel("Moeda padrão").fill("EUR");
    await page.getByLabel("Taxa padrão (%)").fill("23");
    await page.getByLabel("Validade de orçamento (dias)").fill("31");
    await saveFormAndWaitForPut(page, "/settings/me");

    await page.goto("/dashboard/services");
    await page.getByRole("button", { name: "Novo serviço" }).click();
    await page.getByLabel("Nome").fill(serviceName);
    await page.getByLabel("Descrição curta").fill("Serviço criado pelo E2E.");
    await page.getByRole("button", { name: "Guardar serviço" }).click();
    await expect(
      page.locator("article").filter({ hasText: serviceName }),
    ).toBeVisible();

    await page.getByRole("button", { name: "Novo serviço" }).click();
    await page.getByLabel("Nome").fill(secondServiceName);
    await page
      .getByLabel("Descrição curta")
      .fill("Segundo serviço para validar ordenação.");
    await page.getByRole("button", { name: "Guardar serviço" }).click();
    await expect(
      page.locator("article").filter({ hasText: secondServiceName }),
    ).toBeVisible();

    const serviceCard = page
      .locator("article")
      .filter({ hasText: serviceName });
    await serviceCard.getByRole("button", { name: "Editar" }).click();
    await page.getByLabel("Nome").fill(editedServiceName);
    await page.getByRole("button", { name: "Guardar serviço" }).click();
    await expect(
      page.locator("article").filter({ hasText: editedServiceName }),
    ).toBeVisible();

    const editedServiceCard = page
      .locator("article")
      .filter({ hasText: editedServiceName });
    await editedServiceCard
      .getByRole("button", { name: `Mover ${editedServiceName} para baixo` })
      .click();
    await editedServiceCard.getByRole("button", { name: "Desativar" }).click();
    await expect(editedServiceCard.getByText("Inativo")).toBeVisible();
    await editedServiceCard.getByRole("button", { name: "Ativar" }).click();
    await expect(editedServiceCard.getByText("Ativo")).toBeVisible();

    await page.goto("/dashboard/gallery");
    await page.getByRole("button", { name: "Novo item" }).click();
    await page.getByLabel("Título").fill(galleryTitle);
    await page
      .getByLabel("Descrição")
      .fill("Par before/after criado pelo E2E.");
    await page.getByRole("button", { name: "Guardar imagem" }).click();
    const galleryCard = page
      .locator("article")
      .filter({ hasText: galleryTitle });
    await expect(galleryCard).toBeVisible();

    await galleryCard
      .locator('input[type="file"]')
      .nth(0)
      .setInputFiles(imagePath);
    await expect(
      galleryCard.getByRole("button", { name: "Substituir before" }),
    ).toBeVisible();
    await galleryCard
      .locator('input[type="file"]')
      .nth(1)
      .setInputFiles(imagePath);
    await expect(
      galleryCard.getByRole("button", { name: "Substituir after" }),
    ).toBeVisible();
    await galleryCard.getByRole("button", { name: "Destacar" }).click();
    await expect(
      galleryCard.getByText("Destaque", { exact: true }),
    ).toBeVisible();
    await galleryCard.getByRole("button", { name: "Preview" }).click();
    await expect(page.getByRole("dialog")).toContainText(galleryTitle);
    await page.getByRole("button", { name: "Fechar" }).click();

    await expect
      .poll(async () => {
        const response = await request.get(
          `${apiBaseUrl}/public/sites/${auth.company.slug}/services`,
        );
        if (!response.ok()) {
          return false;
        }
        const services = (await response.json()) as Array<{
          name: string;
        }>;
        return services.some((service) => service.name === editedServiceName);
      })
      .toBe(true);

    await expect
      .poll(async () => {
        const response = await request.get(
          `${apiBaseUrl}/public/sites/${auth.company.slug}/gallery`,
        );
        if (!response.ok()) {
          return false;
        }
        const gallery = (await response.json()) as Array<{
          title: string;
          featured: boolean;
          beforeImageUrl: string | null;
          afterImageUrl: string | null;
        }>;
        return gallery.some(
          (item) =>
            item.title === galleryTitle &&
            item.featured &&
            Boolean(item.beforeImageUrl) &&
            Boolean(item.afterImageUrl),
        );
      })
      .toBe(true);
  } finally {
    await deleteCreatedServices(request, auth.accessToken, [
      editedServiceName,
      serviceName,
      secondServiceName,
    ]);
    await deleteCreatedGalleryItems(request, auth.accessToken, [galleryTitle]);
  }
});
