import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BRAND_PALETTES } from "./branding/brand-palettes";
import { BrandingPage } from "./BrandingPage";

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    accessToken: "access-token",
  }),
}));

const company = {
  id: "company-1",
  name: "JR Pinturas",
  tradeName: null,
  slug: "jr-pinturas",
  email: "contato@example.com",
  phone: null,
  whatsapp: null,
  website: null,
  taxNumber: null,
  country: "BR",
  address: null,
  status: "ACTIVE",
};

const branding = {
  id: "branding-1",
  companyId: "company-1",
  logoUrl: null as string | null,
  primaryColor: "#b43f08",
  secondaryColor: null,
  accentColor: "#1c1c1a",
  tagline: "Pintura profissional",
  aboutText: "Texto institucional",
  footerText: null,
  quotationPrefix: "ORC",
  signatureName: "JR Pinturas",
};

function createFetchMock(initialLogoUrl: string | null = null) {
  let currentBranding = { ...branding, logoUrl: initialLogoUrl };

  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = input.toString();
    const method = init?.method ?? "GET";

    if (url.endsWith("/api/company/me") && method === "GET") {
      return Response.json(company);
    }

    if (url.endsWith("/api/branding/me") && method === "GET") {
      return Response.json(currentBranding);
    }

    if (url.endsWith("/api/branding/me") && method === "PUT") {
      currentBranding = {
        ...currentBranding,
        ...(JSON.parse(init?.body as string) as Partial<typeof branding>),
      };
      return Response.json(currentBranding);
    }

    if (url.endsWith("/api/company/logo") && method === "POST") {
      currentBranding = {
        ...currentBranding,
        logoUrl: "/uploads/company/company-1/logo/new-logo.png",
      };
      return Response.json(currentBranding);
    }

    if (url.endsWith("/api/company/logo") && method === "DELETE") {
      currentBranding = { ...currentBranding, logoUrl: null };
      return new Response(null, { status: 204 });
    }

    return new Response(null, { status: 404 });
  });
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BrandingPage />
    </QueryClientProvider>,
  );
}

function getLogoUploadCall(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.find(([input, init]) => {
    return (
      input.toString().endsWith("/api/company/logo") && init?.method === "POST"
    );
  });
}

function getLogoDeleteCall(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.find(([input, init]) => {
    return (
      input.toString().endsWith("/api/company/logo") &&
      init?.method === "DELETE"
    );
  });
}

function getBrandingPutCalls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.filter(([input, init]) => {
    return (
      input.toString().endsWith("/api/branding/me") && init?.method === "PUT"
    );
  });
}

function getBrandingPutPayload(fetchMock: ReturnType<typeof vi.fn>) {
  const call = getBrandingPutCalls(fetchMock).at(-1);
  return JSON.parse(call?.[1]?.body as string) as typeof branding;
}

async function waitForBrandingPage() {
  await screen.findByRole("heading", { name: "Editar identidade visual" });
}

function primaryInput() {
  return screen.getByLabelText("Cor primária") as HTMLInputElement;
}

function secondaryInput() {
  return screen.getByLabelText("Cor secundária") as HTMLInputElement;
}

function accentInput() {
  return screen.getByLabelText("Cor de acento") as HTMLInputElement;
}

function whatsappPreview() {
  return screen.getByText("WhatsApp");
}

function quotePreview() {
  return screen.getByText("Pedir orçamento");
}

describe("BrandingPage color picker", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the three existing color values", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();

    expect(primaryInput()).toHaveValue("#b43f08");
    expect(secondaryInput()).toHaveValue("");
    expect(accentInput()).toHaveValue("#1c1c1a");
  });

  it("opens and closes a color picker", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();
    await user.click(
      screen.getByRole("button", {
        name: "Abrir seletor visual da cor principal",
      }),
    );

    expect(
      screen.getByRole("dialog", { name: "Selecionar cor primária" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Concluir" }));

    expect(
      screen.queryByRole("dialog", { name: "Selecionar cor primária" }),
    ).not.toBeInTheDocument();
  });

  it("closes the color picker with Escape and returns focus to the trigger", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();
    const trigger = screen.getByRole("button", {
      name: "Abrir seletor visual da cor principal",
    });
    await user.click(trigger);

    await user.keyboard("{Escape}");

    expect(
      screen.queryByRole("dialog", { name: "Selecionar cor primária" }),
    ).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it("selects a swatch with the keyboard and updates the preview", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();
    await user.click(
      screen.getByRole("button", {
        name: "Abrir seletor visual da cor principal",
      }),
    );
    const swatch = screen.getByRole("button", { name: "Selecionar #1E40AF" });
    swatch.focus();
    await user.keyboard("{Enter}");

    expect(primaryInput()).toHaveValue("#1E40AF");
    expect(whatsappPreview()).toHaveStyle({ backgroundColor: "#1E40AF" });
  });

  it("accepts manual HEX input and normalizes lowercase on blur", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();
    await user.clear(primaryInput());
    await user.type(primaryInput(), "#0f766e");
    await user.tab();

    expect(primaryInput()).toHaveValue("#0F766E");
    expect(whatsappPreview()).toHaveStyle({ backgroundColor: "#0F766E" });
  });

  it("shows validation for invalid HEX without breaking the preview", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();
    await user.clear(primaryInput());
    await user.type(primaryInput(), "#XYZ");

    expect(whatsappPreview()).toHaveStyle({ backgroundColor: "#1C1C1A" });

    await user.click(screen.getByRole("button", { name: "Guardar" }));

    expect(
      await screen.findByText("Use uma cor HEX válida, ex. #1E40AF."),
    ).toBeInTheDocument();
  });

  it("shows suggested palettes with three colors each", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();

    for (const palette of BRAND_PALETTES) {
      expect(
        screen.getByRole("button", { name: new RegExp(palette.name) }),
      ).toBeInTheDocument();
      expect(
        screen.getByLabelText(`Cores da paleta ${palette.name}`),
      ).toBeInTheDocument();
    }
  });

  it("applies a palette to all three color fields, marks the form dirty, and updates preview without saving", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const palette = BRAND_PALETTES[1];

    renderPage();
    await waitForBrandingPage();
    await user.click(screen.getByRole("button", { name: /Moderna/ }));

    expect(primaryInput()).toHaveValue(palette.primaryColor);
    expect(secondaryInput()).toHaveValue(palette.secondaryColor);
    expect(accentInput()).toHaveValue(palette.accentColor);
    expect(whatsappPreview()).toHaveStyle({
      backgroundColor: palette.primaryColor,
    });
    expect(quotePreview()).toHaveStyle({
      backgroundColor: palette.accentColor,
    });
    expect(screen.getByRole("button", { name: "Guardar" })).toBeEnabled();
    expect(getBrandingPutCalls(fetchMock)).toHaveLength(0);
  });

  it("saves selected palette colors and resets the dirty state from the response", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const palette = BRAND_PALETTES[2];

    renderPage();
    await waitForBrandingPage();
    await user.click(screen.getByRole("button", { name: /Elegante/ }));
    await user.click(screen.getByRole("button", { name: "Guardar" }));

    await waitFor(() => expect(getBrandingPutCalls(fetchMock)).toHaveLength(1));
    expect(getBrandingPutPayload(fetchMock)).toMatchObject({
      primaryColor: palette.primaryColor,
      secondaryColor: palette.secondaryColor,
      accentColor: palette.accentColor,
    });
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Guardar" })).toBeDisabled(),
    );
  });

  it("keeps tagline and about text live preview behavior", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();
    await waitForBrandingPage();
    await user.clear(screen.getByLabelText("Tagline"));
    await user.type(screen.getByLabelText("Tagline"), "Nova chamada");
    await user.clear(screen.getByLabelText("Sobre"));
    await user.type(screen.getByLabelText("Sobre"), "Novo texto institucional");

    expect(screen.getByRole("heading", { name: "Nova chamada" })).toBeVisible();
    expect(screen.getByText("Novo texto institucional")).toBeVisible();
  });
});

describe("BrandingPage logo management", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:3001";
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows the company name fallback when there is no logo", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPage();

    expect(await screen.findByText("Logo da empresa")).toBeInTheDocument();
    expect(screen.getAllByText("JR Pinturas").length).toBeGreaterThan(0);
    expect(
      screen.queryByAltText(/logo atual de jr pinturas/i),
    ).not.toBeInTheDocument();
  });

  it("shows the current logo when logoUrl exists", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchMock("/uploads/company/company-1/logo/current.png"),
    );

    renderPage();

    const logos = await screen.findAllByAltText(/logo atual/i);
    expect(logos[0]).toHaveAttribute(
      "src",
      "http://localhost:3001/uploads/company/company-1/logo/current.png",
    );
    expect(
      screen.getByRole("button", { name: /alterar logo/i }),
    ).toBeInTheDocument();
  });

  it("uploads an initial logo with POST /company/logo and updates the preview", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["logo"], "logo.png", { type: "image/png" });

    renderPage();

    await screen.findByText("Logo da empresa");
    await user.upload(
      screen.getByLabelText(/selecionar logo da empresa/i),
      file,
    );

    await waitFor(() => expect(getLogoUploadCall(fetchMock)).toBeTruthy());
    const uploadCall = getLogoUploadCall(fetchMock);
    const body = uploadCall?.[1]?.body;

    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get("file")).toBe(file);
    expect(
      await screen.findByAltText(/logo atual de jr pinturas/i),
    ).toHaveAttribute(
      "src",
      "http://localhost:3001/uploads/company/company-1/logo/new-logo.png",
    );
    expect(screen.getByAltText("Logo atual")).toHaveAttribute(
      "src",
      "http://localhost:3001/uploads/company/company-1/logo/new-logo.png",
    );
    expect(
      screen.getByRole("button", { name: /alterar logo/i }),
    ).toBeInTheDocument();
  });

  it("reuses POST /company/logo when replacing an existing logo", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock(
      "/uploads/company/company-1/logo/old.png",
    );
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["new"], "new.webp", { type: "image/webp" });

    renderPage();

    await screen.findByRole("button", { name: /alterar logo/i });
    await user.upload(
      screen.getByLabelText(/selecionar logo da empresa/i),
      file,
    );

    await waitFor(() => expect(getLogoUploadCall(fetchMock)).toBeTruthy());
    expect(getLogoUploadCall(fetchMock)?.[0].toString()).toBe(
      "http://localhost:3001/api/company/logo",
    );
    expect(
      (getLogoUploadCall(fetchMock)?.[1]?.body as FormData).get("file"),
    ).toBe(file);
  });

  it("deletes the current logo and returns the preview to the fallback", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock(
      "/uploads/company/company-1/logo/current.png",
    );
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(window, "confirm").mockReturnValue(true);

    renderPage();

    await user.click(
      await screen.findByRole("button", { name: /remover logo/i }),
    );

    await waitFor(() => expect(getLogoDeleteCall(fetchMock)).toBeTruthy());
    expect(getLogoDeleteCall(fetchMock)?.[0].toString()).toBe(
      "http://localhost:3001/api/company/logo",
    );
    expect(
      screen.queryByAltText(/logo atual de jr pinturas/i),
    ).not.toBeInTheDocument();
    expect(screen.getAllByText("JR Pinturas").length).toBeGreaterThan(0);
  });
});
