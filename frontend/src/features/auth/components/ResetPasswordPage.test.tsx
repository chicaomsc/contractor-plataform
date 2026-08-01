import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ResetPasswordPage } from "./ResetPasswordPage";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(window.location.search),
}));

function mockResetResponse(response: Response) {
  return vi.fn(
    async (_input: RequestInfo | URL, _init?: RequestInit) => response,
  );
}

describe("ResetPasswordPage", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
    localStorage.clear();
    sessionStorage.clear();
    window.history.replaceState(null, "", "/reset-password#token=plain-token");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("reads token from hash, clears the URL, and never persists it", async () => {
    vi.stubGlobal(
      "fetch",
      mockResetResponse(
        Response.json({
          message: "Senha atualizada. Acesse sua conta novamente.",
        }),
      ),
    );

    render(<ResetPasswordPage />);

    await waitFor(() => expect(window.location.hash).toBe(""));
    expect(window.location.pathname).toBe("/reset-password");
    expect(localStorage.getItem("plain-token")).toBeNull();
    expect(sessionStorage.getItem("plain-token")).toBeNull();
    expect(document.body).not.toHaveTextContent("plain-token");
  });

  it("shows invalid link state when token is absent", async () => {
    window.history.replaceState(null, "", "/reset-password");

    render(<ResetPasswordPage />);

    expect(await screen.findByText("Link inválido")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /voltar para esqueci minha senha/i }),
    ).toHaveAttribute("href", "/forgot-password");
    expect(
      screen.queryByRole("button", { name: /atualizar password/i }),
    ).not.toBeInTheDocument();
  });

  it("validates divergent password confirmation client-side", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());

    render(<ResetPasswordPage />);

    await user.type(await screen.findByLabelText(/nova senha/i), "Password123");
    await user.type(screen.getByLabelText(/confirmar senha/i), "Different123");
    await user.click(screen.getByRole("button", { name: /atualizar senha/i }));

    expect(
      await screen.findByText("As senhas não coincidem."),
    ).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("submits the reset token and new password without storing a session", async () => {
    const user = userEvent.setup();
    const localStorageSetItem = vi.spyOn(Storage.prototype, "setItem");
    const fetchMock = mockResetResponse(
      Response.json({
        message: "Senha atualizada. Acesse sua conta novamente.",
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<ResetPasswordPage />);

    await user.type(await screen.findByLabelText(/nova senha/i), "Password123");
    await user.type(screen.getByLabelText(/confirmar senha/i), "Password123");
    await user.click(screen.getByRole("button", { name: /atualizar senha/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
      token: "plain-token",
      newPassword: "Password123",
    });
    expect(localStorage.getItem("contractor.accessToken")).toBeNull();
    expect(sessionStorage.length).toBe(0);
    expect(localStorageSetItem).not.toHaveBeenCalled();
    expect(
      await screen.findByText("Senha atualizada. Acesse sua conta novamente."),
    ).toBeInTheDocument();
  });

  it("maps generic 422 reset failures to the generic message", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      mockResetResponse(
        Response.json(
          {
            detail:
              "O link de recuperação é inválido ou não está mais disponível.",
          },
          { status: 422 },
        ),
      ),
    );

    render(<ResetPasswordPage />);

    await user.type(await screen.findByLabelText(/nova senha/i), "Password123");
    await user.type(screen.getByLabelText(/confirmar senha/i), "Password123");
    await user.click(screen.getByRole("button", { name: /atualizar senha/i }));

    expect(
      await screen.findByText(
        "O link de recuperação é inválido ou não está mais disponível.",
      ),
    ).toBeInTheDocument();
  });

  it("shows the backend message for same-password 422", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      mockResetResponse(
        Response.json(
          { detail: "A nova senha deve ser diferente da atual." },
          { status: 422 },
        ),
      ),
    );

    render(<ResetPasswordPage />);

    await user.type(await screen.findByLabelText(/nova senha/i), "Password123");
    await user.type(screen.getByLabelText(/confirmar senha/i), "Password123");
    await user.click(screen.getByRole("button", { name: /atualizar senha/i }));

    expect(
      await screen.findByText("A nova senha deve ser diferente da atual."),
    ).toBeInTheDocument();
  });

  it("returns to admin login after admin-context reset success", async () => {
    window.history.replaceState(
      null,
      "",
      "/reset-password?variant=admin#token=admin-token",
    );

    render(<ResetPasswordPage />);

    expect(
      await screen.findByRole("link", { name: /voltar ao login/i }),
    ).toHaveAttribute("href", "/admin/login");
  });
});
