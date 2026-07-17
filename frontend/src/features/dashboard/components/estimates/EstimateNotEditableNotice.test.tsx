import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EstimateNotEditableNotice } from "./EstimateNotEditableNotice";

describe("EstimateNotEditableNotice", () => {
  it("shows the current status as the reason editing is blocked", () => {
    render(<EstimateNotEditableNotice status="SENT" />);

    expect(screen.getByText("Este orçamento não pode ser editado")).toBeInTheDocument();
    expect(screen.getByText("Enviado")).toBeInTheDocument();
  });

  it("translates each non-editable status to its Portuguese label", () => {
    const { rerender } = render(<EstimateNotEditableNotice status="APPROVED" />);
    expect(screen.getByText("Aprovado")).toBeInTheDocument();

    rerender(<EstimateNotEditableNotice status="COMPLETED" />);
    expect(screen.getByText("Concluído")).toBeInTheDocument();
  });
});
