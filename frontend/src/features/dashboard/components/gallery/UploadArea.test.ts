import { describe, expect, it } from "vitest";
import { validateGalleryImageFile } from "./UploadArea";

describe("validateGalleryImageFile", () => {
  it("accepts safe image formats within the maximum size", () => {
    const file = new File(["image"], "photo.png", { type: "image/png" });

    expect(validateGalleryImageFile(file)).toBeNull();
  });

  it("rejects unsupported formats, mismatched extensions and oversized files", () => {
    const textFile = new File(["text"], "file.txt", { type: "text/plain" });
    const svgFile = new File(["<svg />"], "vector.svg", {
      type: "image/svg+xml",
    });
    const mismatchedFile = new File(["image"], "photo.gif", {
      type: "image/png",
    });
    const largeImage = new File(
      [new Uint8Array(5 * 1024 * 1024 + 1)],
      "x.jpg",
      {
        type: "image/jpeg",
      },
    );

    expect(validateGalleryImageFile(textFile)).toContain("Formato inválido");
    expect(validateGalleryImageFile(svgFile)).toContain("Formato inválido");
    expect(validateGalleryImageFile(mismatchedFile)).toContain(
      "Formato inválido",
    );
    expect(validateGalleryImageFile(largeImage)).toContain("tamanho máximo");
  });
});
