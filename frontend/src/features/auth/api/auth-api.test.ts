import { beforeEach, describe, expect, it, vi } from "vitest";
import { ZodError } from "zod";
import { acceptTeamInvitation } from "./auth-api";

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  window.localStorage.clear();
  vi.restoreAllMocks();
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function authResponse(role: "MANAGER" | "MEMBER" | "ADMIN") {
  return {
    accessToken: "access-token",
    refreshToken: "refresh-token",
    user: {
      id: "user-1",
      companyId: "company-1",
      email: "collaborator@example.test",
      name: "Collaborator",
      role,
      status: "ACTIVE",
    },
    company: {
      id: "company-1",
      name: "Acme",
      slug: "acme",
      email: null,
      country: "PT",
      status: "ACTIVE",
    },
  };
}

describe("auth API team invitation acceptance", () => {
  it.each(["MANAGER", "MEMBER"] as const)(
    "posts only token, name and password and accepts %s AuthResponse",
    async (role) => {
      const fetchMock = vi.fn().mockResolvedValue(jsonResponse(authResponse(role)));
      vi.stubGlobal("fetch", fetchMock);

      await expect(
        acceptTeamInvitation({
          token: "team-token",
          name: "New User",
          password: "SecurePass123!",
        }),
      ).resolves.toMatchObject({
        accessToken: "access-token",
        user: { role },
      });

      const [url, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
      expect(url.toString()).toBe(
        "http://api.test/api/auth/team-invitations/accept",
      );
      expect(init.method).toBe("POST");
      expect(
        (init.headers as Headers).has("Authorization"),
      ).toBeFalsy();
      expect(JSON.parse(init.body as string)).toEqual({
        token: "team-token",
        name: "New User",
        password: "SecurePass123!",
      });
      expect(init.body as string).not.toContain("email");
      expect(init.body as string).not.toContain("companyId");
      expect(init.body as string).not.toContain("role");
      expect(init.body as string).not.toContain("confirmPassword");
    },
  );

  it("rejects an invalid role in AuthResponse through the strict role schema", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(jsonResponse(authResponse("ADMIN"))),
    );

    await expect(
      acceptTeamInvitation({
        token: "team-token",
        name: "New User",
        password: "SecurePass123!",
      }),
    ).rejects.toBeInstanceOf(ZodError);
  });
});
