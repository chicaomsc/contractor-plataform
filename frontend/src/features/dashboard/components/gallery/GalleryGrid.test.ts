import { describe, expect, it } from "vitest";
import type { GalleryDto } from "../../types/admin";
import { sortGalleryItems } from "./GalleryGrid";

function galleryItem(
  id: string,
  title: string,
  displayOrder: number,
): GalleryDto {
  return {
    id,
    companyId: "company-1",
    title,
    description: null,
    beforeImageUrl: null,
    afterImageUrl: null,
    displayOrder,
    featured: false,
    active: true,
    createdAt: "2026-07-16T00:00:00Z",
    updatedAt: "2026-07-16T00:00:00Z",
  };
}

describe("sortGalleryItems", () => {
  it("sorts by display order and then title", () => {
    const items = [
      galleryItem("3", "Zona exterior", 2),
      galleryItem("1", "Cozinha", 1),
      galleryItem("2", "Banho", 1),
    ];

    expect(sortGalleryItems(items).map((item) => item.title)).toEqual([
      "Banho",
      "Cozinha",
      "Zona exterior",
    ]);
  });
});
