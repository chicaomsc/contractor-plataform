import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AcceptInvitePage } from "./AcceptInvitePage";

function mockAuthResponse(response: Response) {
  return vi.fn(
    async (_input: RequestInfo | URL, _init?: RequestInit) => response,
  );
}

const authResponseBody = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  user: {
    id: "user-1",
    companyId: "company-1",
    email: "owner@example.com",
    name: "Owner",
    role: "OWNER",
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

describe("AcceptInvitePage", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
    localStorage.clear();
    sessionStorage.clear();
    window.history.replaceState(null, "", "/invite#token=plain-invite-token");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("reads the token from the URL fragment, clears the URL, and never persists it in storage", async () => {
    vi.stubGlobal("fetch", mockAuthResponse(Response.json(authResponseBody)));

    render(<AcceptInvitePage />);

    await waitFor(() => expect(window.location.hash).toBe(""));
    expect(window.location.pathname).toBe("/invite");
    expect(localStorage.getItem("plain-invite-token")).toBeNull();
    expect(sessionStorage.getItem("plain-invite-token")).toBeNull();
    expect(document.body).not.toHaveTextContent("plain-invite-token");
  });

  it("disables the submit button when no token is present", async () => {
    window.history.replaceState(null, "", "/invite");

    render(<AcceptInvitePage />);

    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Ativar conta" }),
      ).toBeDisabled(),
    );
    expect(
      screen.getByText("O link de convite está incompleto."),
    ).toBeInTheDocument();
  });

  it("validates divergent password confirmation client-side", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());

    render(<AcceptInvitePage />);

    await user.type(await screen.findByLabelText("Password"), "Password123");
    await user.type(
      screen.getByLabelText("Confirmar password"),
      "Different123",
    );
    await user.click(screen.getByRole("button", { name: "Ativar conta" }));

    expect(
      await screen.findByText("As passwords não coincidem."),
    ).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("rejects a password shorter than the shared policy minimum", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());

    render(<AcceptInvitePage />);

    await user.type(await screen.findByLabelText("Password"), "short1");
    await user.type(screen.getByLabelText("Confirmar password"), "short1");
    await user.click(screen.getByRole("button", { name: "Ativar conta" }));

    expect(
      await screen.findByText("Use pelo menos 8 caracteres."),
    ).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("submits the invite token and password, then redirects to the dashboard", async () => {
    const user = userEvent.setup();
    const fetchMock = mockAuthResponse(Response.json(authResponseBody));
    vi.stubGlobal("fetch", fetchMock);

    render(<AcceptInvitePage />);

    // The fragment must be read from the real jsdom location first — only then is
    // window.location safe to replace with a static stub carrying an assign() spy,
    // since jsdom's location.assign is non-configurable and can't be spied in place.
    await waitFor(() => expect(window.location.hash).toBe(""));
    const originalLocation = window.location;
    const assignSpy = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...originalLocation, assign: assignSpy },
    });

    try {
      await user.type(await screen.findByLabelText("Password"), "Password123");
      await user.type(
        screen.getByLabelText("Confirmar password"),
        "Password123",
      );
      await user.click(screen.getByRole("button", { name: "Ativar conta" }));

      await waitFor(() => expect(fetchMock).toHaveBeenCalled());
      expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
        token: "plain-invite-token",
        password: "Password123",
      });
      await waitFor(() =>
        expect(assignSpy).toHaveBeenCalledWith("/dashboard"),
      );
    } finally {
      Object.defineProperty(window, "location", {
        configurable: true,
        value: originalLocation,
      });
    }
  });

  it("maps a 422 accept failure to the expired/used message", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      mockAuthResponse(
        Response.json({ detail: "Invite token is expired" }, { status: 422 }),
      ),
    );

    render(<AcceptInvitePage />);

    await user.type(await screen.findByLabelText("Password"), "Password123");
    await user.type(
      screen.getByLabelText("Confirmar password"),
      "Password123",
    );
    await user.click(screen.getByRole("button", { name: "Ativar conta" }));

    expect(
      await screen.findByText("Este convite expirou, foi revogado ou já foi usado."),
    ).toBeInTheDocument();
  });
});
