import { describe, expect, it } from "vitest";
import { getBrandingStyle, isSafeHexColor } from "./branding";
import {
  mapPublicGalleryItemDto,
  mapPublicServiceDto,
  mapPublicSiteDto,
} from "./public-site";

describe("public site mappers", () => {
  it("maps site DTO into a view model with safe branding", () => {
    const vm = mapPublicSiteDto({
      slug: "empresa-teste",
      name: "Empresa Teste Lda",
      tradeName: "Empresa Teste",
      publicPhone: "+351 910 000 000",
      whatsapp: null,
      website: null,
      location: { city: "Porto", region: "Norte", country: "PT" },
      branding: {
        logoUrl: "https://cdn.test/logo.png",
        primaryColor: "#123456",
        secondaryColor: null,
        accentColor: "not-css",
        tagline: null,
        aboutText: null,
        footerText: null,
      },
    });

    expect(vm.displayName).toBe("Empresa Teste");
    expect(vm.locationLabel).toBe("Porto, Norte, PT");
    expect(vm.tagline).toBeNull();
    expect(vm.aboutText).toBeNull();
    expect(vm.branding.primaryColor).toBe("#123456");
    expect(vm.branding.accentColor).toBeNull();
    expect(getBrandingStyle(vm)).toMatchObject({ "--primary": "#123456" });
  });

  it("validates only full hex color tokens", () => {
    expect(isSafeHexColor("#E8500A")).toBe(true);
    expect(isSafeHexColor("#fff")).toBe(false);
    expect(isSafeHexColor("var(--primary)")).toBe(false);
  });

  it("maps service DTO into a compact view model", () => {
    expect(
      mapPublicServiceDto({
        id: "service-1",
        name: "Serviço",
        slug: "servico",
        shortDescription: null,
        description: "Descrição longa",
        icon: null,
        displayOrder: 2,
      }),
    ).toMatchObject({ summary: "Descrição longa", displayOrder: 2 });
  });

  it("maps gallery before/after availability", () => {
    expect(
      mapPublicGalleryItemDto({
        id: "gallery-1",
        title: "Obra",
        description: null,
        beforeImageUrl: "https://cdn.test/before.jpg",
        afterImageUrl: null,
        displayOrder: 0,
        featured: false,
      }),
    ).toMatchObject({
      beforeImageUrl: "https://cdn.test/before.jpg",
      beforeAlt: "Obra antes da intervenção",
      hasBeforeImage: true,
      hasAfterImage: false,
      hasCompleteBeforeAfterPair: false,
    });
  });

  it("resolves relative upload URLs against the public API base URL", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8081";
    process.env.NEXT_PUBLIC_COMPANY_SLUG = "empresa-teste";

    const site = mapPublicSiteDto({
      slug: "empresa-teste",
      name: "Empresa Teste",
      tradeName: null,
      publicPhone: null,
      whatsapp: null,
      website: null,
      location: null,
      branding: {
        logoUrl: "/uploads/company/logo.png",
        primaryColor: null,
        secondaryColor: null,
        accentColor: null,
        tagline: null,
        aboutText: null,
        footerText: null,
      },
    });

    const gallery = mapPublicGalleryItemDto({
      id: "gallery-1",
      title: "Obra",
      description: null,
      beforeImageUrl: "/uploads/company/before.jpg",
      afterImageUrl: null,
      displayOrder: 0,
      featured: false,
    });

    expect(site.branding.logoUrl).toBe(
      "http://localhost:8081/uploads/company/logo.png",
    );
    expect(gallery.beforeImageUrl).toBe(
      "http://localhost:8081/uploads/company/before.jpg",
    );
  });
});
