import { expect, test, type Page } from "@playwright/test";

const frontendPort = process.env.E2E_FRONTEND_PORT ?? "3001";
const frontendOrigin = `http://jr-pinturas.localhost:${frontendPort}`;
const now = "2026-07-26T10:00:00Z";

const ownerSession = {
  user: {
    id: "owner-user",
    companyId: "owner-company",
    email: "owner@example.test",
    name: "Owner Test",
    role: "OWNER",
    status: "ACTIVE",
  },
  company: {
    id: "owner-company",
    name: "JR Pinturas",
    slug: "jr-pinturas",
    email: null,
    country: "PT",
    status: "ACTIVE",
  },
  branding: null,
  settings: null,
};

const adminSession = {
  user: {
    id: "admin-user",
    companyId: null,
    email: "admin@example.test",
    name: "Platform Admin",
    role: "SUPER_ADMIN",
    status: "ACTIVE",
  },
  company: null,
  branding: null,
  settings: null,
};

async function authenticate(page: Page, role: "OWNER" | "SUPER_ADMIN") {
  await page.context().addCookies([
    { name: "contractor_session", value: "active", url: frontendOrigin },
    { name: "contractor_role", value: role, url: frontendOrigin },
  ]);

  await page.addInitScript(() => {
    window.localStorage.setItem("contractor.accessToken", "layout-token");
  });
}

async function mockDashboard(page: Page) {
  await authenticate(page, "OWNER");
  await page.route("**/api/auth/me", (route) =>
    route.fulfill({ json: ownerSession }),
  );
  await page.route("**/api/company/me", (route) =>
    route.fulfill({
      json: {
        id: "owner-company",
        name: "JR Pinturas",
        tradeName: "JR Pinturas",
        slug: "jr-pinturas",
        email: "owner@example.test",
        phone: null,
        whatsapp: null,
        website: null,
        taxNumber: null,
        country: "PT",
        address: {
          street: "Rua Principal",
          city: "Lisboa",
          postalCode: "1000",
          region: "Lisboa",
          country: "PT",
        },
        status: "ACTIVE",
      },
    }),
  );
}

async function mockAdmin(page: Page) {
  await authenticate(page, "SUPER_ADMIN");
  await page.route("**/api/auth/me", (route) =>
    route.fulfill({ json: adminSession }),
  );
  await page.route("**/api/admin/companies?**", (route) =>
    route.fulfill({
      json: {
        content: Array.from({ length: 40 }, (_, index) => ({
          id: `company-${index}`,
          name: `Company ${index}`,
          slug: `company-${index}`,
          status: index % 2 === 0 ? "ACTIVE" : "INACTIVE",
          ownerEmail: `owner${index}@example.test`,
          createdAt: now,
        })),
        totalElements: 40,
        totalPages: 4,
        size: 10,
        number: 0,
        first: true,
        last: false,
      },
    }),
  );
}

async function makePageLong(page: Page) {
  await page.evaluate(() => {
    const spacer = document.createElement("div");
    spacer.style.height = "1600px";
    spacer.setAttribute("data-layout-spacer", "true");
    document.querySelector("main")?.appendChild(spacer);
  });
}

async function expectSidebarFooterPinned(
  page: Page,
  footerTestId: string,
) {
  await makePageLong(page);

  const viewport = page.viewportSize();
  expect(viewport).not.toBeNull();

  const scrollHeight = await page.evaluate(
    () => document.documentElement.scrollHeight,
  );
  expect(scrollHeight).toBeGreaterThan(viewport!.height);

  await page.evaluate((top) => window.scrollTo(0, top), scrollHeight / 2);
  await page.waitForTimeout(50);

  const footer = page.getByTestId(footerTestId);
  const box = await footer.boundingBox();
  expect(box).not.toBeNull();

  const distanceFromViewportBottom = Math.abs(
    viewport!.height - (box!.y + box!.height),
  );
  expect(distanceFromViewportBottom).toBeLessThanOrEqual(2);
}

test("dashboard sidebar account footer stays pinned during long-page scroll", async ({
  page,
}) => {
  await mockDashboard(page);
  await page.goto("/dashboard/company");
  await expectSidebarFooterPinned(page, "dashboard-sidebar-account");
});

test("admin sidebar account footer stays pinned during long-page scroll", async ({
  page,
}) => {
  await mockAdmin(page);
  await page.goto("/admin/companies");
  await expectSidebarFooterPinned(page, "admin-sidebar-account");
});
