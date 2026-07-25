import { defineConfig, devices } from "@playwright/test";

const isCI = Boolean(process.env.CI);
const frontendPort = process.env.E2E_FRONTEND_PORT ?? "3001";
const backendPort = process.env.E2E_BACKEND_PORT ?? "8081";
const frontendUrl = `http://127.0.0.1:${frontendPort}`;
const backendUrl = `http://127.0.0.1:${backendPort}`;

export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  retries: isCI ? 1 : 0,
  workers: 1,
  reporter: isCI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: frontendUrl,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
    browserName: "chromium",
    channel: isCI ? undefined : "chrome",
  },
  webServer: [
    {
      command: `docker compose -f ../docker/docker-compose.yml up -d postgres && cd ../backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=${backendPort} --app.cors.allowed-origins=${frontendUrl}"`,
      url: `${backendUrl}/actuator/health`,
      reuseExistingServer: true,
      timeout: 120_000,
      stdout: "ignore",
      stderr: "pipe",
    },
    {
      // NEXT_PUBLIC_API_BASE_URL is the frontend's OWN origin, not the backend's —
      // matching the same-origin-via-Caddy architecture (Sprint 11A.3): the browser
      // calls withApiPrefix()-prefixed paths against this origin, expecting /api/*
      // to reach the backend. There's no Caddy in this local/E2E setup to do that
      // stripping, so API_PROXY_TARGET tells next.config.ts's dev-only rewrite where
      // the real backend is — see next.config.ts for why this is safe in production
      // (API_PROXY_TARGET is never set there, so the rewrite is a no-op).
      command: `NEXT_PUBLIC_API_BASE_URL=${frontendUrl} NEXT_PUBLIC_SITE_URL=${frontendUrl} NEXT_PUBLIC_COMPANY_SLUG=e2e-hardening API_PROXY_TARGET=${backendUrl} npm run dev -- --hostname 127.0.0.1 --port ${frontendPort}`,
      url: `${frontendUrl}/login`,
      reuseExistingServer: true,
      timeout: 120_000,
      stdout: "ignore",
      stderr: "pipe",
    },
  ],
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "mobile-smoke",
      testMatch: /smoke\.spec\.ts/,
      use: { ...devices["Pixel 5"] },
    },
  ],
});
