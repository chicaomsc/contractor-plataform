import { afterEach, describe, expect, it } from "vitest";
import { getPublicEnv } from "./public-env";

const originalEnv = process.env;

afterEach(() => {
  process.env = { ...originalEnv };
});

describe("getPublicEnv", () => {
  it("returns validated public env values", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    process.env.NEXT_PUBLIC_SITE_URL = "http://localhost:3000";

    expect(getPublicEnv()).toMatchObject({
      NEXT_PUBLIC_API_BASE_URL: "http://localhost:8080",
      NEXT_PUBLIC_SITE_URL: "http://localhost:3000",
    });
  });

  it("throws when the API URL is invalid", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "not-a-url";

    expect(() => getPublicEnv()).toThrow();
  });

  it("does not require a build-time company slug", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    delete process.env["NEXT_PUBLIC_" + "COMPANY_" + "SLUG"];

    expect(getPublicEnv()).toMatchObject({
      NEXT_PUBLIC_API_BASE_URL: "http://localhost:8080",
    });
  });
});
