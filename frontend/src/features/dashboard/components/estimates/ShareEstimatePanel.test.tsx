import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/errors";
import { ShareEstimatePanel } from "./ShareEstimatePanel";

const createMutateAsync = vi.fn();
const revokeMutateAsync = vi.fn().mockResolvedValue(undefined);
let createIsPending = false;
let createIsError = false;
let revokeIsPending = false;
let revokeIsError = false;
let shareQueryState: {
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  data: unknown;
} = { isLoading: false, isError: true, error: new ApiError("not found", 404), data: undefined };

vi.mock("../../hooks/estimate-share-hooks", () => ({
  useEstimateShare: () => shareQueryState,
  useCreateEstimateShare: () => ({
    mutateAsync: createMutateAsync,
    get isPending() {
      return createIsPending;
    },
    get isError() {
      return createIsError;
    },
  }),
  useRevokeEstimateShare: () => ({
    mutateAsync: revokeMutateAsync,
    get isPending() {
      return revokeIsPending;
    },
    get isError() {
      return revokeIsError;
    },
  }),
}));

describe("ShareEstimatePanel", () => {
  beforeEach(() => {
    createMutateAsync.mockReset();
    revokeMutateAsync.mockClear();
    createIsPending = false;
    createIsError = false;
    revokeIsPending = false;
    revokeIsError = false;
    shareQueryState = { isLoading: false, isError: true, error: new ApiError("not found", 404), data: undefined };
    vi.stubGlobal("navigator", { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("shows a loading indicator while checking for an existing share", () => {
    shareQueryState = { isLoading: true, isError: false, error: null, data: undefined };
    render(<ShareEstimatePanel estimateId="e1" />);

    expect(screen.getByText(/A verificar link de partilha/)).toBeInTheDocument();
  });

  it("offers to create a share when none exists yet (404)", () => {
    render(<ShareEstimatePanel estimateId="e1" />);

    expect(screen.getByText("Compartilhar")).toBeInTheDocument();
  });

  it("creating a share reveals the link, copy button and WhatsApp button", async () => {
    createMutateAsync.mockResolvedValue({
      id: "share1",
      status: "ACTIVE",
      token: "abc123token",
      createdAt: "2026-07-16T10:00:00Z",
      expiresAt: "2026-08-15T10:00:00Z",
      revokedAt: null,
      lastAccessAt: null,
      accessCount: 0,
    });
    shareQueryState = {
      isLoading: false,
      isError: false,
      error: null,
      data: {
        id: "share1",
        status: "ACTIVE",
        token: null,
        createdAt: "2026-07-16T10:00:00Z",
        expiresAt: "2026-08-15T10:00:00Z",
        revokedAt: null,
        lastAccessAt: null,
        accessCount: 0,
      },
    };

    render(<ShareEstimatePanel estimateId="e1" />);

    await act(async () => {
      fireEvent.click(screen.getByText("Gerar novo link"));
    });

    expect(createMutateAsync).toHaveBeenCalledWith({});
    const input = screen.getByLabelText("Link público do orçamento") as HTMLInputElement;
    expect(input.value).toContain("/share/abc123token");
    expect(screen.getByText("Copiar link")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /WhatsApp/ })).toHaveAttribute(
      "href",
      expect.stringContaining("https://wa.me/?text="),
    );
  });

  it("copy button writes the link to the clipboard", async () => {
    createMutateAsync.mockResolvedValue({
      id: "share1",
      status: "ACTIVE",
      token: "copy-token",
      createdAt: "2026-07-16T10:00:00Z",
      expiresAt: "2026-08-15T10:00:00Z",
      revokedAt: null,
      lastAccessAt: null,
      accessCount: 0,
    });
    shareQueryState = {
      isLoading: false,
      isError: false,
      error: null,
      data: {
        id: "share1",
        status: "ACTIVE",
        token: null,
        createdAt: "2026-07-16T10:00:00Z",
        expiresAt: "2026-08-15T10:00:00Z",
        revokedAt: null,
        lastAccessAt: null,
        accessCount: 0,
      },
    };

    render(<ShareEstimatePanel estimateId="e1" />);
    await act(async () => {
      fireEvent.click(screen.getByText("Gerar novo link"));
    });
    await act(async () => {
      fireEvent.click(screen.getByText("Copiar link"));
    });

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
      expect.stringContaining("/share/copy-token"),
    );
    expect(screen.getByText("Copiado")).toBeInTheDocument();
  });

  it("an active share without a revealed token offers to regenerate or revoke, without showing a link input", () => {
    shareQueryState = {
      isLoading: false,
      isError: false,
      error: null,
      data: {
        id: "share1",
        status: "ACTIVE",
        token: null,
        createdAt: "2026-07-16T10:00:00Z",
        expiresAt: "2026-08-15T10:00:00Z",
        revokedAt: null,
        lastAccessAt: "2026-07-17T10:00:00Z",
        accessCount: 3,
      },
    };

    render(<ShareEstimatePanel estimateId="e1" />);

    expect(screen.queryByLabelText("Link público do orçamento")).not.toBeInTheDocument();
    expect(screen.getByText("Gerar novo link")).toBeInTheDocument();
    expect(screen.getByText("Revogar")).toBeInTheDocument();
    expect(screen.getByText(/Acedido 3 vezes/)).toBeInTheDocument();
  });

  it("revoke calls the revoke mutation", async () => {
    shareQueryState = {
      isLoading: false,
      isError: false,
      error: null,
      data: {
        id: "share1",
        status: "ACTIVE",
        token: null,
        createdAt: "2026-07-16T10:00:00Z",
        expiresAt: "2026-08-15T10:00:00Z",
        revokedAt: null,
        lastAccessAt: null,
        accessCount: 0,
      },
    };

    render(<ShareEstimatePanel estimateId="e1" />);
    await act(async () => {
      fireEvent.click(screen.getByText("Revogar"));
    });

    expect(revokeMutateAsync).toHaveBeenCalled();
  });

  it("shows an error state when creation fails", () => {
    createIsError = true;
    render(<ShareEstimatePanel estimateId="e1" />);

    expect(screen.getByText(/Não foi possível criar o link de partilha/)).toBeInTheDocument();
  });
});
