import { QueryClientProvider } from "@tanstack/react-query";
import { act, render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createQueryClient } from "@/lib/query/query-client";
import { expireSession, markSessionActive } from "../api/session-lifecycle";
import { AuthProvider, useAuth } from "./auth-context";

const replaceMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: replaceMock }),
}));

function renderAuthProvider(children: React.ReactNode) {
  const queryClient = createQueryClient();

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>,
  );
}

function LogoutButton() {
  const { logout } = useAuth();

  return (
    <button type="button" onClick={() => void logout()}>
      Logout
    </button>
  );
}

beforeEach(() => {
  markSessionActive();
  window.localStorage.clear();
  window.history.pushState(null, "", "/dashboard");
  replaceMock.mockClear();
});

afterEach(() => {
  cleanup();
  markSessionActive();
  window.localStorage.clear();
  replaceMock.mockClear();
});

describe("AuthProvider session expiration", () => {
  it("shows exactly one session expired modal for repeated expiration signals", async () => {
    renderAuthProvider(<div>Dashboard</div>);

    act(() => {
      expireSession();
      expireSession();
    });

    expect(
      await screen.findByRole("dialog", { name: "Sua sessão expirou" }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole("dialog")).toHaveLength(1);
    expect(
      screen.getByText(
        "Por segurança, sua sessão foi encerrada. Entre novamente para continuar.",
      ),
    ).toBeInTheDocument();
  });

  it("navigates to /login when the user confirms the expired session modal", async () => {
    const user = userEvent.setup();
    renderAuthProvider(<div>Dashboard</div>);

    act(() => {
      expireSession();
    });
    const loginButton = await screen.findByRole("button", {
      name: "Ir para o login",
    });

    await user.click(loginButton);

    expect(replaceMock).toHaveBeenCalledWith("/login");
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  it("does not show the session expired modal on login routes", () => {
    window.history.pushState(null, "", "/login");
    renderAuthProvider(<div>Login</div>);

    act(() => {
      expireSession();
    });

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("does not emit or display session expiration on manual logout", async () => {
    const user = userEvent.setup();
    const expiredListener = vi.fn();
    window.addEventListener("contractor:session-expired", expiredListener);
    renderAuthProvider(<LogoutButton />);

    await user.click(screen.getByRole("button", { name: "Logout" }));

    expect(expiredListener).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(replaceMock).toHaveBeenCalledWith("/login");
    window.removeEventListener("contractor:session-expired", expiredListener);
  });
});
