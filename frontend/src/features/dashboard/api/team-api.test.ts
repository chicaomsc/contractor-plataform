import { beforeEach, describe, expect, it, vi } from "vitest";
import { ZodError } from "zod";
import {
  createTeamInvitation,
  fetchTeamInvitations,
  fetchTeamMembers,
  removeTeamMember,
  resendTeamInvitation,
  revokeTeamInvitation,
  updateTeamMemberRole,
} from "./team-api";

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  vi.restoreAllMocks();
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const member = {
  id: "user-1",
  name: "Maria",
  email: "maria@example.test",
  role: "MEMBER",
  status: "ACTIVE",
  createdAt: "2026-09-10T10:00:00Z",
};

const invitation = {
  id: "invite-1",
  email: "joao@example.test",
  role: "MANAGER",
  status: "PENDING",
  expiresAt: "2026-09-17T10:00:00Z",
  createdAt: "2026-09-10T10:00:00Z",
};

describe("team API", () => {
  it("fetches and validates team members", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse([member])));

    await expect(fetchTeamMembers("access-token")).resolves.toEqual([member]);
  });

  it("fetches and validates team invitations", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(jsonResponse([invitation])),
    );

    await expect(fetchTeamInvitations("access-token")).resolves.toEqual([
      invitation,
    ]);
  });

  it("rejects invalid member roles returned by the backend", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(jsonResponse([{ ...member, role: "ADMIN" }])),
    );

    await expect(fetchTeamMembers("access-token")).rejects.toBeInstanceOf(
      ZodError,
    );
  });

  it("rejects unexpected sensitive invitation fields", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(jsonResponse([{ ...invitation, token: "secret" }])),
    );

    await expect(fetchTeamInvitations("access-token")).rejects.toBeInstanceOf(
      ZodError,
    );
  });

  it("creates invitations with a trimmed email and assignable role only", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(invitation, 201));
    vi.stubGlobal("fetch", fetchMock);

    await createTeamInvitation("access-token", {
      email: "  joao@example.test  ",
      role: "MANAGER",
    });

    const [, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(JSON.parse(init.body as string)).toEqual({
      email: "joao@example.test",
      role: "MANAGER",
    });
  });

  it("rejects OWNER as an invitation role before sending the request", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createTeamInvitation("access-token", {
        email: "owner@example.test",
        role: "OWNER",
      } as never),
    ).rejects.toBeInstanceOf(ZodError);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("updates a member role with PATCH", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ ...member, role: "MANAGER" }));
    vi.stubGlobal("fetch", fetchMock);

    await updateTeamMemberRole("access-token", "user-1", { role: "MANAGER" });

    const [url, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(url.toString()).toBe(
      "http://api.test/api/team/members/user-1/role",
    );
    expect(init.method).toBe("PATCH");
    expect(JSON.parse(init.body as string)).toEqual({ role: "MANAGER" });
  });

  it("removes, resends and revokes through the expected endpoints", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse(invitation, 201))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await removeTeamMember("access-token", "user-1");
    await resendTeamInvitation("access-token", "invite-1");
    await revokeTeamInvitation("access-token", "invite-1");

    expect((fetchMock.mock.calls[0][0] as URL).pathname).toBe(
      "/api/team/members/user-1",
    );
    expect(fetchMock.mock.calls[0][1].method).toBe("DELETE");
    expect((fetchMock.mock.calls[1][0] as URL).pathname).toBe(
      "/api/team/invitations/invite-1/resend",
    );
    expect(fetchMock.mock.calls[1][1].method).toBe("POST");
    expect((fetchMock.mock.calls[2][0] as URL).pathname).toBe(
      "/api/team/invitations/invite-1",
    );
    expect(fetchMock.mock.calls[2][1].method).toBe("DELETE");
  });
});
