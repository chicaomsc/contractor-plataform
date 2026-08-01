import { expect, test } from "@playwright/test";
import {
  apiBaseUrl,
  loginAdminViaUi,
  loginViaUi,
  platformAdminEmail,
  platformAdminPassword,
  registerAccount,
} from "./helpers";

async function requestForgotPasswordViaUi(
  page: import("@playwright/test").Page,
  email: string,
  admin = false,
) {
  await page.goto(
    admin ? "/forgot-password?variant=admin" : "/forgot-password",
  );
  await page.getByLabel("Email").fill(email);
  await page.getByRole("button", { name: /Enviar instruções/ }).click();
  await expect(
    page.getByText(
      "Se existir uma conta para este e-mail, as instruções foram geradas.",
    ),
  ).toBeVisible();
  const debugLink = page.getByRole("link", {
    name: /Abrir link de recuperação/,
  });
  await expect(debugLink).toBeVisible();
  const href = await debugLink.getAttribute("href");
  expect(href).toContain(
    admin ? "/reset-password?variant=admin#token=" : "/reset-password#token=",
  );
  return href as string;
}

async function resetPasswordViaUi(
  page: import("@playwright/test").Page,
  resetLink: string,
  newPassword: string,
) {
  await page.goto(resetLink);
  await expect(page).toHaveURL((url) => url.hash === "");
  await page.getByLabel("Nova senha").fill(newPassword);
  await page.getByLabel("Confirmar password").fill(newPassword);
  await page.getByRole("button", { name: /Atualizar password/ }).click();
  await expect(
    page.getByText("Senha atualizada. Acesse sua conta novamente."),
  ).toBeVisible();
}

test("OWNER solicita reset, define nova senha e login antigo falha", async ({
  page,
  request,
}) => {
  const suffix = `recovery.owner.${Date.now()}`;
  const { email, password } = await registerAccount(request, suffix);
  const newPassword = "RecoveredOwner123!";

  const resetLink = await requestForgotPasswordViaUi(page, email);
  await resetPasswordViaUi(page, resetLink, newPassword);

  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page.getByText("Sessão expirada")).toBeVisible();

  await loginViaUi(page, email, newPassword);
});

test("SUPER_ADMIN usa o mesmo fluxo e retorna para admin login", async ({
  page,
}) => {
  const temporaryPassword = `RecoveredAdmin123!${Date.now()}`;

  const resetLink = await requestForgotPasswordViaUi(
    page,
    platformAdminEmail,
    true,
  );
  await resetPasswordViaUi(page, resetLink, temporaryPassword);
  await expect(
    page.getByRole("link", { name: /Voltar ao login/ }),
  ).toHaveAttribute("href", "/admin/login");

  await page.goto("/admin/login");
  await page.getByLabel("Email").fill(platformAdminEmail);
  await page.getByLabel("Password").fill(temporaryPassword);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/admin/);

  const restoreLink = await requestForgotPasswordViaUi(
    page,
    platformAdminEmail,
    true,
  );
  await resetPasswordViaUi(page, restoreLink, platformAdminPassword);
});

test("SUPER_ADMIN gera link manual para OWNER ACTIVE no detalhe da Company", async ({
  page,
  request,
}) => {
  const suffix = `recovery.admin.${Date.now()}`;
  const { auth, email, password } = await registerAccount(request, suffix);
  const newPassword = "AdminIssuedOwner123!";

  await loginAdminViaUi(page);
  await page.goto(`/admin/companies/${auth.company.id}`);
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: /Gerar link de recuperação/ }).click();

  const resetLink = await page
    .getByRole("textbox", { name: "Link de recuperação" })
    .inputValue();
  expect(resetLink).toContain("/reset-password#token=");

  await resetPasswordViaUi(page, resetLink, newPassword);

  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page.getByText("Sessão expirada")).toBeVisible();

  await loginViaUi(page, email, newPassword);
});

test("forgot password keeps nonexistent accounts indistinguishable", async ({
  page,
}) => {
  await page.goto("/forgot-password");
  await page.getByLabel("Email").fill(`missing.${Date.now()}@contractor.test`);
  await page.getByRole("button", { name: /Enviar instruções/ }).click();

  await expect(
    page.getByText(
      "Se existir uma conta para este e-mail, as instruções foram geradas.",
    ),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: /Abrir link de recuperação/ }),
  ).toHaveCount(0);

  const response = await page.request.get(
    `${apiBaseUrl}/public/sites/tenant-inexistente-e2e`,
  );
  expect(response.status()).toBe(404);
});
