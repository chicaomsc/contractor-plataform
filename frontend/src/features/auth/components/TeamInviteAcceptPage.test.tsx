import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { TeamInviteAcceptPage } from "./TeamInviteAcceptPage";

function mockAuthResponse(response: Response) {
  return vi.fn(
    async (_input: RequestInfo | URL, _init?: RequestInit) => response,
  );
}

const managerAuthResponseBody = {
  accessToken: "access-manager",
  refreshToken: "refresh-manager",
  user: {
    id: "manager-1",
    companyId: "company-1",
    email: "manager@example.test",
    name: "Manager User",
    role: "MANAGER",
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

const memberAuthResponseBody = {
  ...managerAuthResponseBody,
  accessToken: "access-member",
  refreshToken: "refresh-member",
  user: {
    ...managerAuthResponseBody.user,
    id: "member-1",
    email: "member@example.test",
    name: "Member User",
    role: "MEMBER",
  },
};

async function replaceLocationWithAssignSpy() {
  await waitFor(() => expect(window.location.hash).toBe(""));
  const originalLocation = window.location;
  const assignSpy = vi.fn();
  Object.defineProperty(window, "location", {
    configurable: true,
    value: { ...originalLocation, assign: assignSpy },
  });
  return { originalLocation, assignSpy };
}

describe("TeamInviteAcceptPage", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
    localStorage.clear();
    sessionStorage.clear();
    document.cookie = "contractor_session=; path=/; max-age=0; SameSite=Lax";
    document.cookie = "contractor_role=; path=/; max-age=0; SameSite=Lax";
    window.history.replaceState(null, "", "/invite/team#token=plain-team-token");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("captures the token from the fragment, clears the URL, enables the form and never persists the token", async () => {
    const consoleLog = vi.spyOn(console, "log").mockImplementation(() => {});
    vi.stubGlobal("fetch", mockAuthResponse(Response.json(managerAuthResponseBody)));

    render(<TeamInviteAcceptPage />);

    await waitFor(() => expect(window.location.hash).toBe(""));
    expect(window.location.pathname).toBe("/invite/team");
    expect(screen.getByRole("textbox", { name: "Nome" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Criar acesso" })).toBeEnabled();
    expect(localStorage.getItem("plain-team-token")).toBeNull();
    expect(sessionStorage.getItem("plain-team-token")).toBeNull();
    expect(document.body).not.toHaveTextContent("plain-team-token");
    expect(window.location.href).not.toContain("plain-team-token");
    expect(consoleLog).not.toHaveBeenCalledWith(
      expect.stringContaining("plain-team-token"),
    );
  });

  it("shows an invalid state and no usable form when the token is missing", async () => {
    window.history.replaceState(null, "", "/invite/team");

    render(<TeamInviteAcceptPage />);

    expect(await screen.findByRole("heading", { name: "Convite inválido" }))
      .toBeVisible();
    expect(
      screen.getByText("Este convite é inválido ou não está mais disponível."),
    ).toBeVisible();
    expect(screen.queryByRole("textbox", { name: "Nome" })).toBeNull();
    expect(screen.getByRole("link", { name: "Ir para o login" })).toHaveAttribute(
      "href",
      "/login",
    );
  });

  it("does not accept a token from the query string", async () => {
    window.history.replaceState(null, "", "/invite/team?token=query-token");

    render(<TeamInviteAcceptPage />);

    expect(await screen.findByRole("heading", { name: "Convite inválido" }))
      .toBeVisible();
    expect(window.location.href).not.toContain("?token=query-token");
    expect(screen.queryByRole("textbox", { name: "Nome" })).toBeNull();
    expect(localStorage.getItem("query-token")).toBeNull();
    expect(sessionStorage.getItem("query-token")).toBeNull();
  });

  it("validates name, password policy and matching confirmation before submitting", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());

    render(<TeamInviteAcceptPage />);

    await user.click(await screen.findByRole("button", { name: "Criar acesso" }));

    expect(
      await screen.findByText("Indique pelo menos 2 caracteres."),
    ).toBeVisible();
    expect(await screen.findByText("Use pelo menos 8 caracteres.")).toBeVisible();
    expect(fetch).not.toHaveBeenCalled();

    await user.type(screen.getByRole("textbox", { name: /Nome/ }), "Maria");
    await user.type(screen.getByLabelText(/^Senha/), "Password123");
    await user.type(screen.getByLabelText(/Confirmar senha/), "Different123");
    await user.click(screen.getByRole("button", { name: "Criar acesso" }));

    expect(await screen.findByText("As senhas não coincidem.")).toBeVisible();
    expect(fetch).not.toHaveBeenCalled();
  });

  it.each([
    ["MANAGER", managerAuthResponseBody, "access-manager", "refresh-manager"],
    ["MEMBER", memberAuthResponseBody, "access-member", "refresh-member"],
  ] as const)(
    "persists a %s AuthResponse and redirects to dashboard",
    async (_role, authResponse, accessToken, refreshToken) => {
      const user = userEvent.setup();
      const fetchMock = mockAuthResponse(Response.json(authResponse));
      vi.stubGlobal("fetch", fetchMock);

      render(<TeamInviteAcceptPage />);
      const { originalLocation, assignSpy } =
        await replaceLocationWithAssignSpy();

      try {
        await user.type(await screen.findByRole("textbox", { name: "Nome" }), "Maria");
        await user.type(screen.getByLabelText(/^Senha/), "Password123");
        await user.type(screen.getByLabelText(/Confirmar senha/), "Password123");
        await user.click(screen.getByRole("button", { name: "Criar acesso" }));

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
        expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
          token: "plain-team-token",
          name: "Maria",
          password: "Password123",
        });
        expect(localStorage.getItem("contractor.accessToken")).toBe(accessToken);
        expect(localStorage.getItem("contractor.refreshToken")).toBe(
          refreshToken,
        );
        expect(localStorage.getItem("contractor.user")).toContain(
          authResponse.user.role,
        );
        expect(localStorage.getItem("plain-team-token")).toBeNull();
        expect(sessionStorage.getItem("plain-team-token")).toBeNull();
        await waitFor(() =>
          expect(assignSpy).toHaveBeenCalledWith("/dashboard"),
        );
      } finally {
        Object.defineProperty(window, "location", {
          configurable: true,
          value: originalLocation,
        });
      }
    },
  );

  it("collapses public invalid, expired, used and revoked failures to one safe message", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      mockAuthResponse(
        Response.json(
          { detail: "O convite é inválido ou não está mais disponível." },
          { status: 422 },
        ),
      ),
    );

    render(<TeamInviteAcceptPage />);

    await user.type(await screen.findByRole("textbox", { name: "Nome" }), "Maria");
    await user.type(screen.getByLabelText(/^Senha/), "Password123");
    await user.type(screen.getByLabelText(/Confirmar senha/), "Password123");
    await user.click(screen.getByRole("button", { name: "Criar acesso" }));

    expect(
      await screen.findByText(
        "Este convite é inválido ou não está mais disponível.",
      ),
    ).toBeVisible();
  });
});
