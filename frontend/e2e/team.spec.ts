import { expect, test } from "@playwright/test";
import { loginViaUi, registerAccount } from "./helpers";

test("OWNER opens team page and creates a collaborator invitation", async ({
  page,
  request,
}) => {
  const suffix = `team.${Date.now()}`;
  const { email, password, auth } = await registerAccount(request, suffix);
  const inviteEmail = `collaborator.${suffix}@contractor.test`;

  await loginViaUi(page, email, password);
  await page.getByRole("link", { name: "Equipe" }).click();

  await expect(page).toHaveURL(/\/dashboard\/team/);
  await expect(page.getByRole("heading", { name: "Equipe" })).toBeVisible();
  await expect(
    page.getByRole("cell", { name: auth.user.name }).first(),
  ).toBeVisible();
  await expect(
    page.getByRole("cell", { name: "Proprietário" }).first(),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /Remover da equipe/ }),
  ).toHaveCount(0);

  await page.getByRole("button", { name: "Convidar colaborador" }).click();
  await page.getByLabel("E-mail").fill(inviteEmail);
  await page.getByLabel("Função").selectOption("MANAGER");

  const createResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith("/team/invitations") &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Enviar convite" }).click();
  await expect((await createResponse).status()).toBe(201);

  await expect(page.getByText(inviteEmail)).toBeVisible();
  await expect(page.getByText("Administrador")).toBeVisible();
});
