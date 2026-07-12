import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ApiUnavailable } from "./ApiUnavailable";

describe("ApiUnavailable", () => {
  it("announces the fallback state", () => {
    render(<ApiUnavailable />);

    expect(screen.getByRole("status")).toHaveTextContent("valores de fallback");
  });
});
