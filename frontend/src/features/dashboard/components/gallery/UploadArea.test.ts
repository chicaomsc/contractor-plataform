import { describe, expect, it } from "vitest";
import { validateGalleryImageFile } from "./UploadArea";

describe("validateGalleryImageFile", () => {
  it("accepts image files within the maximum size", () => {
    const file = new File(["image"], "photo.png", { type: "image/png" });

    expect(validateGalleryImageFile(file)).toBeNull();
  });

  it("rejects unsupported formats and oversized files", () => {
    const textFile = new File(["text"], "file.txt", { type: "text/plain" });
    const largeImage = new File([new Uint8Array(5 * 1024 * 1024 + 1)], "x.jpg", {
      type: "image/jpeg",
    });

    expect(validateGalleryImageFile(textFile)).toContain("Formato inválido");
    expect(validateGalleryImageFile(largeImage)).toContain("tamanho máximo");
  });
});
