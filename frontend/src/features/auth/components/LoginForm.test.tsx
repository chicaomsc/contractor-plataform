import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LoginForm } from "./LoginForm";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock("../hooks/auth-context", () => ({
  useAuth: () => ({
    login: vi.fn(),
  }),
}));

describe("LoginForm", () => {
  it("links owner login to password recovery", () => {
    render(<LoginForm />);

    expect(screen.getByRole("link", { name: /esqueci minha senha/i })).toHaveAttribute(
      "href",
      "/forgot-password",
    );
  });

  it("links admin login to password recovery with admin context", () => {
    render(<LoginForm variant="admin" />);

    expect(screen.getByRole("link", { name: /esqueci minha senha/i })).toHaveAttribute(
      "href",
      "/forgot-password?variant=admin",
    );
  });
});
