import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { CustomerDto } from "../../types/customers";
import { CustomerSelector } from "./CustomerSelector";

const refetch = vi.fn();
let mockQueryState: {
  data: CustomerDto[] | undefined;
  isLoading: boolean;
  isError: boolean;
};

vi.mock("../../hooks/customer-hooks", () => ({
  useCustomers: () => ({ ...mockQueryState, refetch }),
}));

function customer(overrides: Partial<CustomerDto> & Pick<CustomerDto, "id" | "name">): CustomerDto {
  return {
    id: overrides.id,
    companyId: "c1",
    name: overrides.name,
    email: overrides.email ?? null,
    phone: overrides.phone ?? null,
    taxNumber: null,
    address: null,
    notes: null,
    active: overrides.active ?? true,
    createdAt: "2026-07-16T10:00:00Z",
    updatedAt: "2026-07-16T10:00:00Z",
  };
}

describe("CustomerSelector", () => {
  it("lists only active customers and lets the user select one", () => {
    mockQueryState = {
      isLoading: false,
      isError: false,
      data: [
        customer({ id: "1", name: "Jane Doe", email: "jane@example.com" }),
        customer({ id: "2", name: "Inactive Co", active: false }),
      ],
    };

    const onSelect = vi.fn();
    render(
      <CustomerSelector
        selectedCustomerId={null}
        onSelect={onSelect}
        onCreateNew={vi.fn()}
      />,
    );

    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    expect(screen.queryByText("Inactive Co")).not.toBeInTheDocument();

    fireEvent.click(screen.getByText("Jane Doe"));
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "1", name: "Jane Doe" }),
    );
  });

  it("filters customers by search term (name, email or phone)", () => {
    mockQueryState = {
      isLoading: false,
      isError: false,
      data: [
        customer({ id: "1", name: "Jane Doe", email: "jane@example.com" }),
        customer({ id: "2", name: "Bob Silva", phone: "912345678" }),
      ],
    };

    render(
      <CustomerSelector selectedCustomerId={null} onSelect={vi.fn()} onCreateNew={vi.fn()} />,
    );

    fireEvent.change(screen.getByLabelText("Pesquisar cliente"), {
      target: { value: "bob" },
    });

    expect(screen.queryByText("Jane Doe")).not.toBeInTheDocument();
    expect(screen.getByText("Bob Silva")).toBeInTheDocument();
  });

  it("shows loading state while fetching", () => {
    mockQueryState = { isLoading: true, isError: false, data: undefined };

    render(
      <CustomerSelector selectedCustomerId={null} onSelect={vi.fn()} onCreateNew={vi.fn()} />,
    );

    expect(screen.getByText("A carregar clientes")).toBeInTheDocument();
  });

  it("calls onCreateNew when the quick-create button is clicked", () => {
    mockQueryState = { isLoading: false, isError: false, data: [] };
    const onCreateNew = vi.fn();

    render(
      <CustomerSelector selectedCustomerId={null} onSelect={vi.fn()} onCreateNew={onCreateNew} />,
    );

    fireEvent.click(screen.getByText("Criar cliente"));
    expect(onCreateNew).toHaveBeenCalled();
  });
});
