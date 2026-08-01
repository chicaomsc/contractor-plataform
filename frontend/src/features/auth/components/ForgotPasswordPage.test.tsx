import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ForgotPasswordPage } from "./ForgotPasswordPage";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(window.location.search),
}));

function mockForgotResponse(body: Record<string, unknown>) {
  return vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
    Response.json(body),
  );
}

describe("ForgotPasswordPage", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
    window.history.replaceState(null, "", "/forgot-password");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("submits forgot password and shows the generic anti-enumeration response", async () => {
    const user = userEvent.setup();
    const fetchMock = mockForgotResponse({
      message: "Se existir uma conta para este email, as instruções foram geradas.",
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), "owner@example.com");
    await user.click(screen.getByRole("button", { name: /enviar instruções/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(fetchMock.mock.calls[0][0].toString()).toBe(
      "http://localhost:3001/api/auth/password/forgot",
    );
    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
      email: "owner@example.com",
    });
    expect(
      screen.getByText("Se existir uma conta para este e-mail, as instruções foram geradas."),
    ).toBeInTheDocument();
  });

  it("keeps the same success UX for responses without debug data", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      mockForgotResponse({
        message: "Se existir uma conta para este email, as instruções foram geradas.",
      }),
    );

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), "missing@example.com");
    await user.click(screen.getByRole("button", { name: /enviar instruções/i }));

    expect(
      await screen.findByText(
        "Se existir uma conta para este e-mail, as instruções foram geradas.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText(/desenvolvimento local/i)).not.toBeInTheDocument();
  });

  it("shows an optional local debug reset link when returned by the backend", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      mockForgotResponse({
        message: "Se existir uma conta para este email, as instruções foram geradas.",
        debugToken: "debug-token",
        debugResetLink: "http://localhost:3001/reset-password#token=debug-token",
      }),
    );

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), "owner@example.com");
    await user.click(screen.getByRole("button", { name: /enviar instruções/i }));

    expect(await screen.findByText(/desenvolvimento local/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /abrir link de recuperação/i })).toHaveAttribute(
      "href",
      "http://localhost:3001/reset-password#token=debug-token",
    );
  });

  it("preserves the admin login return context", () => {
    window.history.replaceState(null, "", "/forgot-password?variant=admin");

    render(<ForgotPasswordPage />);

    expect(screen.getByRole("link", { name: /voltar ao login/i })).toHaveAttribute(
      "href",
      "/admin/login",
    );
  });

  it("adds admin context to the optional debug reset link", async () => {
    const user = userEvent.setup();
    window.history.replaceState(null, "", "/forgot-password?variant=admin");
    vi.stubGlobal(
      "fetch",
      mockForgotResponse({
        message: "Se existir uma conta para este email, as instruções foram geradas.",
        debugResetLink: "http://localhost:3001/reset-password#token=admin-token",
      }),
    );

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), "admin@example.com");
    await user.click(screen.getByRole("button", { name: /enviar instruções/i }));

    expect(screen.getByRole("link", { name: /abrir link de recuperação/i })).toHaveAttribute(
      "href",
      "http://localhost:3001/reset-password?variant=admin#token=admin-token",
    );
  });
});
