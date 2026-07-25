import { describe, expect, it } from "vitest";
import { GET } from "./route";

describe("GET /api/health", () => {
  it("responds 200 with a minimal UP body", async () => {
    const response = GET();

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({ status: "UP" });
  });

  it("never exposes environment variables, version, or hostname", async () => {
    const response = GET();
    const body = await response.json();

    expect(Object.keys(body)).toEqual(["status"]);
  });
});
