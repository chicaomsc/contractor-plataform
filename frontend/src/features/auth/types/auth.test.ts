import { describe, expect, it } from "vitest";
import { authResponseSchema, meResponseSchema, userRoleSchema } from "./auth";

function authResponse(role: "SUPER_ADMIN" | "OWNER" | "MANAGER" | "MEMBER") {
  return {
    accessToken: "access-token",
    refreshToken: "refresh-token",
    user: {
      id: "user-1",
      companyId: role === "SUPER_ADMIN" ? null : "company-1",
      email: "user@example.test",
      name: "User",
      role,
      status: "ACTIVE",
    },
    company:
      role === "SUPER_ADMIN"
        ? null
        : {
            id: "company-1",
            name: "Company",
            slug: "company",
            email: null,
            country: "PT",
            status: "ACTIVE",
          },
  };
}

describe("auth schemas", () => {
  it("accepts every supported technical role", () => {
    expect(userRoleSchema.options).toEqual([
      "SUPER_ADMIN",
      "OWNER",
      "MANAGER",
      "MEMBER",
    ]);

    for (const role of userRoleSchema.options) {
      expect(authResponseSchema.parse(authResponse(role)).user.role).toBe(role);
      expect(
        meResponseSchema.parse({
          ...authResponse(role),
          branding: null,
          settings: null,
        }).user.role,
      ).toBe(role);
    }
  });

  it("keeps role validation strict", () => {
    expect(() => userRoleSchema.parse("ADMIN")).toThrow();
  });
});
