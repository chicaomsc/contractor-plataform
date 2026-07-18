import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { DownloadPdfButton } from "./DownloadPdfButton";

const mutateAsync = vi.fn();
let isPending = false;
let isError = false;

vi.mock("../../hooks/estimate-hooks", () => ({
  useDownloadEstimatePdf: () => ({
    mutateAsync,
    get isPending() {
      return isPending;
    },
    get isError() {
      return isError;
    },
  }),
}));

describe("DownloadPdfButton", () => {
  let createObjectURL: ReturnType<typeof vi.fn>;
  let revokeObjectURL: ReturnType<typeof vi.fn>;
  let clickSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    isPending = false;
    isError = false;
    mutateAsync.mockReset();

    createObjectURL = vi.fn().mockReturnValue("blob:mock-url");
    revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", { ...URL, createObjectURL, revokeObjectURL });

    clickSpy = vi.fn();
    HTMLAnchorElement.prototype.click = clickSpy;
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("shows the download button by default", () => {
    render(<DownloadPdfButton estimateId="e1" estimateNumber="ORC-2026-0001" />);

    expect(screen.getByText("Baixar PDF")).toBeInTheDocument();
    expect(screen.getByRole("button")).not.toBeDisabled();
  });

  it("downloads the blob and triggers a browser save using the server-provided filename", async () => {
    mutateAsync.mockResolvedValue({
      blob: new Blob(["%PDF-1.4"], { type: "application/pdf" }),
      filename: "orcamento-ORC-2026-0001.pdf",
    });

    render(<DownloadPdfButton estimateId="e1" estimateNumber="ORC-2026-0001" />);
    fireEvent.click(screen.getByRole("button"));

    await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith("e1"));
    await waitFor(() => expect(clickSpy).toHaveBeenCalled());

    expect(createObjectURL).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:mock-url");
  });

  it("falls back to a generated filename when Content-Disposition provided none", async () => {
    mutateAsync.mockResolvedValue({
      blob: new Blob(["%PDF-1.4"], { type: "application/pdf" }),
      filename: null,
    });

    render(<DownloadPdfButton estimateId="e1" estimateNumber="ORC-2026-0001" />);
    fireEvent.click(screen.getByRole("button"));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
  });

  it("shows a loading state and disables the button while the request is in flight", () => {
    isPending = true;
    render(<DownloadPdfButton estimateId="e1" estimateNumber="ORC-2026-0001" />);

    expect(screen.getByText("A gerar PDF")).toBeInTheDocument();
    expect(screen.getByRole("button")).toBeDisabled();
  });

  it("does not start a second download while one is already pending", () => {
    isPending = true;
    render(<DownloadPdfButton estimateId="e1" estimateNumber="ORC-2026-0001" />);

    fireEvent.click(screen.getByRole("button"));

    expect(mutateAsync).not.toHaveBeenCalled();
  });

  it("shows a friendly error message when the download fails", () => {
    isError = true;
    render(<DownloadPdfButton estimateId="e1" estimateNumber="ORC-2026-0001" />);

    expect(screen.getByText(/Não foi possível gerar o PDF/)).toBeInTheDocument();
  });
});
