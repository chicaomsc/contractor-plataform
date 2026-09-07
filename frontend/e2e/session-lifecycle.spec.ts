import { expect, test, type Page } from "@playwright/test";
import { registerAccount, type AuthSession } from "./helpers";

async function installBrowserSession(
  page: Page,
  auth: AuthSession,
  accessToken: string,
  refreshToken: string,
) {
  await page.addInitScript(
    ({ session, nextAccessToken, nextRefreshToken }) => {
      window.localStorage.setItem("contractor.accessToken", nextAccessToken);
      window.localStorage.setItem("contractor.refreshToken", nextRefreshToken);
      window.localStorage.setItem(
        "contractor.user",
        JSON.stringify(session.user),
      );
      window.localStorage.setItem(
        "contractor.company",
        JSON.stringify(session.company),
      );
      document.cookie = "contractor_session=active; path=/; SameSite=Lax";
      document.cookie = `contractor_role=${session.user.role}; path=/; SameSite=Lax`;
    },
    {
      session: auth,
      nextAccessToken: accessToken,
      nextRefreshToken: refreshToken,
    },
  );
}

test("renova silenciosamente o access token expirado e mantém o usuário na página", async ({
  page,
  request,
}) => {
  const suffix = `session-refresh.${Date.now()}`;
  const { auth } = await registerAccount(request, suffix);
  let refreshCount = 0;

  await page.route("**/api/auth/refresh", async (route) => {
    refreshCount += 1;
    await route.continue();
  });
  await installBrowserSession(
    page,
    auth,
    "invalid-expired-access-token",
    auth.refreshToken,
  );

  await page.goto("/dashboard/services");

  await expect(page).toHaveURL(/\/dashboard\/services/);
  await expect(
    page.getByRole("heading", { name: "Gerenciar serviços" }),
  ).toBeVisible();
  expect(refreshCount).toBe(1);
  await expect
    .poll(() =>
      page.evaluate(() =>
        window.localStorage.getItem("contractor.accessToken"),
      ),
    )
    .not.toBe("invalid-expired-access-token");
});

test("mostra modal quando refresh inválido não consegue renovar a sessão", async ({
  page,
  request,
}) => {
  const suffix = `session-expired.${Date.now()}`;
  const { auth } = await registerAccount(request, suffix);
  let refreshCount = 0;

  await page.route("**/api/auth/refresh", async (route) => {
    refreshCount += 1;
    await route.continue();
  });
  await installBrowserSession(
    page,
    auth,
    "invalid-expired-access-token",
    "invalid-refresh-token",
  );

  await page.goto("/dashboard/services");

  await expect(
    page.getByRole("dialog", { name: "Sua sessão expirou" }),
  ).toBeVisible();
  expect(refreshCount).toBe(1);
  await expect
    .poll(() =>
      page.evaluate(() =>
        window.localStorage.getItem("contractor.accessToken"),
      ),
    )
    .toBeNull();

  await page.getByRole("button", { name: "Ir para o login" }).click();

  await expect(page).toHaveURL(/\/login/);
});
