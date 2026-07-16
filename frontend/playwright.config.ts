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
      command: `NEXT_PUBLIC_API_BASE_URL=${backendUrl} NEXT_PUBLIC_SITE_URL=${frontendUrl} NEXT_PUBLIC_COMPANY_SLUG=e2e-hardening npm run dev -- --hostname 127.0.0.1 --port ${frontendPort}`,
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
