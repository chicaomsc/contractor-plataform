import { describe, expect, it } from "vitest";
import type { ServiceDto } from "../../types/admin";
import { sortServices } from "./ServiceList";

function service(
  overrides: Partial<ServiceDto> & Pick<ServiceDto, "id" | "name">,
): ServiceDto {
  return {
    id: overrides.id,
    companyId: "company-id",
    name: overrides.name,
    slug: overrides.name.toLowerCase().replaceAll(" ", "-"),
    shortDescription: null,
    description: null,
    icon: null,
    displayOrder: overrides.displayOrder ?? 0,
    active: overrides.active ?? true,
    createdAt: "2026-07-16T10:00:00.000Z",
    updatedAt: "2026-07-16T10:00:00.000Z",
  };
}

describe("sortServices", () => {
  it("sorts by display order and then by localized name", () => {
    const services = [
      service({ id: "3", name: "Ladrilhos", displayOrder: 2 }),
      service({ id: "1", name: "Pintura", displayOrder: 1 }),
      service({ id: "2", name: "Acabamento", displayOrder: 2 }),
    ];

    expect(sortServices(services).map((item) => item.name)).toEqual([
      "Pintura",
      "Acabamento",
      "Ladrilhos",
    ]);
  });
});
