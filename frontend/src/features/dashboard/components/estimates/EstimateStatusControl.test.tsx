import { act, fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EstimateStatusControl } from "./EstimateStatusControl";

const mutateAsync = vi.fn().mockResolvedValue(undefined);
let isPending = false;
let isError = false;

vi.mock("../../hooks/estimate-hooks", () => ({
  useChangeEstimateStatus: () => ({
    mutateAsync,
    get isPending() {
      return isPending;
    },
    get isError() {
      return isError;
    },
  }),
}));

describe("EstimateStatusControl", () => {
  it("offers every status except the current one", () => {
    isPending = false;
    isError = false;
    render(<EstimateStatusControl estimateId="e1" currentStatus="DRAFT" />);

    const select = screen.getByLabelText("Novo status") as HTMLSelectElement;
    const optionLabels = Array.from(select.options).map((option) => option.textContent);

    expect(optionLabels).toContain("Enviado");
    expect(optionLabels).toContain("Cancelado");
    expect(optionLabels).not.toContain("Rascunho");
  });

  it("submits the selected status via the status-change endpoint", async () => {
    isPending = false;
    isError = false;
    render(<EstimateStatusControl estimateId="e1" currentStatus="DRAFT" />);

    fireEvent.change(screen.getByLabelText("Novo status"), {
      target: { value: "SENT" },
    });
    await act(async () => {
      fireEvent.click(screen.getByText("Confirmar"));
    });

    expect(mutateAsync).toHaveBeenCalledWith({ estimateId: "e1", status: "SENT" });
  });

  it("disables the confirm button until a status is chosen", () => {
    isPending = false;
    isError = false;
    render(<EstimateStatusControl estimateId="e1" currentStatus="DRAFT" />);

    expect(screen.getByText("Confirmar")).toBeDisabled();
  });

  it("surfaces a backend rejection (e.g. invalid transition, 409) without re-implementing the state machine", () => {
    isPending = false;
    isError = true;
    render(<EstimateStatusControl estimateId="e1" currentStatus="DRAFT" />);

    expect(
      screen.getByText(/Não foi possível aplicar esta transição de status/),
    ).toBeInTheDocument();
  });
});
