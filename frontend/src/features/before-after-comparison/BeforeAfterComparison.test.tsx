import { createElement } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeAll } from "vitest";
import { BeforeAfterComparisonBaseline } from "./BeforeAfterComparisonBaseline";
import { BeforeAfterComparisonRefinedCandidate } from "./BeforeAfterComparisonRefinedCandidate";
import { beforeAfterComparisonFixture } from "./fixtures";
import type { BeforeAfterComparisonProps } from "./types";

vi.mock("next/image", () => ({
  default: ({
    src,
    alt,
    fill: _fill,
    sizes: _sizes,
    ...props
  }: {
    src: string;
    alt: string;
    fill?: boolean;
    sizes?: string;
    [key: string]: unknown;
  }) => createElement("img", { src, alt, ...props }),
}));

const variants = [
  {
    name: "baseline",
    Component: BeforeAfterComparisonBaseline,
    stageId: "baseline-comparison-stage",
    sliderId: "baseline-comparison-slider",
    label: /baseline/i,
  },
  {
    name: "refined candidate",
    Component: BeforeAfterComparisonRefinedCandidate,
    stageId: "refined-candidate-comparison-stage",
    sliderId: "refined-candidate-comparison-slider",
    label: /candidato refinado/i,
  },
];

beforeAll(() => {
  Object.defineProperty(Element.prototype, "setPointerCapture", {
    value: vi.fn(),
    configurable: true,
  });
  Object.defineProperty(Element.prototype, "hasPointerCapture", {
    value: vi.fn(() => true),
    configurable: true,
  });
  Object.defineProperty(Element.prototype, "releasePointerCapture", {
    value: vi.fn(),
    configurable: true,
  });
});

describe.each(variants)(
  "BeforeAfterComparison $name",
  ({ Component, stageId, sliderId, label }) => {
    function renderComponent(props: Partial<BeforeAfterComparisonProps> = {}) {
      return render(<Component {...beforeAfterComparisonFixture} {...props} />);
    }

    it("renders images, labels and optional content", () => {
      renderComponent();

      expect(
        screen.getByRole("heading", { name: /fixture técnica de comparação/i }),
      ).toBeInTheDocument();
      expect(screen.getAllByText("Antes").length).toBeGreaterThan(0);
      expect(screen.getAllByText("Depois").length).toBeGreaterThan(0);
      expect(
        screen.getAllByAltText(/parede de teste antes da intervenção/i).length,
      ).toBeGreaterThan(0);
      expect(
        screen.getAllByAltText(/mesma parede de teste depois da intervenção/i)
          .length,
      ).toBeGreaterThan(0);
    });

    it("supports keyboard navigation and boundaries", async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.tab();
      const slider = screen.getByTestId(sliderId);
      expect(slider).toHaveFocus();
      expect(slider).toHaveAttribute("aria-valuenow", "50");

      fireEvent.keyDown(slider, { key: "ArrowRight" });
      expect(slider).toHaveAttribute("aria-valuenow", "55");

      fireEvent.keyDown(slider, { key: "ArrowLeft" });
      expect(slider).toHaveAttribute("aria-valuenow", "50");

      fireEvent.keyDown(slider, { key: "Home" });
      expect(slider).toHaveAttribute("aria-valuenow", "0");

      fireEvent.keyDown(slider, { key: "End" });
      expect(slider).toHaveAttribute("aria-valuenow", "100");

      await user.tab({ shift: true });
      expect(slider).not.toHaveFocus();
    });

    it("updates to an intermediate pointer position", async () => {
      renderComponent();
      const stage = screen.getByTestId(stageId);
      vi.spyOn(stage, "getBoundingClientRect").mockReturnValue({
        x: 0,
        y: 0,
        width: 200,
        height: 150,
        top: 0,
        right: 200,
        bottom: 150,
        left: 0,
        toJSON: () => "",
      });

      const pointerDown = new MouseEvent("pointerdown", {
        bubbles: true,
        clientX: 50,
      });
      Object.defineProperty(pointerDown, "pointerId", {
        value: 1,
      });

      fireEvent(stage, pointerDown);
      await waitFor(() => {
        expect(screen.getByTestId(sliderId)).toHaveAttribute(
          "aria-valuenow",
          "25",
        );
      });
    });

    it("clamps minimum and maximum values", () => {
      renderComponent({ initialPosition: -20 });
      expect(screen.getByTestId(sliderId)).toHaveAttribute(
        "aria-valuenow",
        "0",
      );

      render(
        <Component {...beforeAfterComparisonFixture} initialPosition={140} />,
      );
      expect(
        screen.getAllByRole("slider", { name: label }).at(-1),
      ).toHaveAttribute("aria-valuenow", "100");
    });

    it("supports optional title and description", () => {
      renderComponent({ title: null, description: null });

      expect(
        screen.queryByRole("heading", { name: /fixture técnica/i }),
      ).not.toBeInTheDocument();
      expect(screen.getByRole("slider", { name: label })).toBeInTheDocument();
    });

    it("renders an accessible fallback when one image is missing", () => {
      renderComponent({ afterImageUrl: null, afterAlt: null });

      expect(screen.getByRole("status")).toHaveTextContent(/comparação/i);
      expect(
        screen.queryByRole("slider", { name: label }),
      ).not.toBeInTheDocument();
    });

    it("keeps motion requirements explicit", () => {
      renderComponent();
      const slider = screen.getByTestId(sliderId);

      expect(slider.className).not.toContain("transition-all");
    });

    it("does not render tenant-specific content", () => {
      const { container } = renderComponent();

      expect(container).not.toHaveTextContent(/jr pinturas|jr-pinturas/i);
    });
  },
);
