import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const apiBaseUrl = process.env.E2E_API_URL ?? "http://127.0.0.1:8081";

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    name: string;
  };
  company: {
    id: string;
    name: string;
    slug: string;
  };
};

export async function registerAccount(
  request: APIRequestContext,
  suffix = Date.now().toString(),
) {
  const email = `e2e.${suffix}@contractor.test`;
  const password = "SecurePass123!";
  const response = await request.post(`${apiBaseUrl}/auth/register`, {
    data: {
      ownerName: `E2E Owner ${suffix}`,
      email,
      password,
      companyName: "E2E Hardening",
      country: "PT",
    },
  });

  expect(response.status()).toBe(201);

  const auth = (await response.json()) as AuthSession;
  return { auth, email, password };
}

export async function loginViaUi(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText("Status do site")).toBeVisible();
}

export async function deleteCreatedServices(
  request: APIRequestContext,
  accessToken: string,
  names: string[],
) {
  const response = await request.get(`${apiBaseUrl}/services`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (!response.ok()) {
    return;
  }

  const services = (await response.json()) as Array<{
    id: string;
    name: string;
  }>;
  await Promise.all(
    services
      .filter((service) => names.includes(service.name))
      .map((service) =>
        request.delete(`${apiBaseUrl}/services/${service.id}`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        }),
      ),
  );
}

export async function deleteCreatedGalleryItems(
  request: APIRequestContext,
  accessToken: string,
  titles: string[],
) {
  const response = await request.get(`${apiBaseUrl}/gallery`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (!response.ok()) {
    return;
  }

  const items = (await response.json()) as Array<{ id: string; title: string }>;
  await Promise.all(
    items
      .filter((item) => titles.includes(item.title))
      .map((item) =>
        request.delete(`${apiBaseUrl}/gallery/${item.id}`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        }),
      ),
  );
}
