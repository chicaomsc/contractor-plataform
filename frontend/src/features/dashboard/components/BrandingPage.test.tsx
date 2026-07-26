import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
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
    return input.toString().endsWith("/api/company/logo") && init?.method === "POST";
  });
}

function getLogoDeleteCall(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.find(([input, init]) => {
    return input.toString().endsWith("/api/company/logo") && init?.method === "DELETE";
  });
}

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
    expect(screen.queryByAltText(/logo atual de jr pinturas/i)).not.toBeInTheDocument();
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
    expect(screen.getByRole("button", { name: /alterar logo/i })).toBeInTheDocument();
  });

  it("uploads an initial logo with POST /company/logo and updates the preview", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["logo"], "logo.png", { type: "image/png" });

    renderPage();

    await screen.findByText("Logo da empresa");
    await user.upload(screen.getByLabelText(/selecionar logo da empresa/i), file);

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
    expect(screen.getByRole("button", { name: /alterar logo/i })).toBeInTheDocument();
  });

  it("reuses POST /company/logo when replacing an existing logo", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock("/uploads/company/company-1/logo/old.png");
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["new"], "new.webp", { type: "image/webp" });

    renderPage();

    await screen.findByRole("button", { name: /alterar logo/i });
    await user.upload(screen.getByLabelText(/selecionar logo da empresa/i), file);

    await waitFor(() => expect(getLogoUploadCall(fetchMock)).toBeTruthy());
    expect(getLogoUploadCall(fetchMock)?.[0].toString()).toBe(
      "http://localhost:3001/api/company/logo",
    );
    expect((getLogoUploadCall(fetchMock)?.[1]?.body as FormData).get("file")).toBe(file);
  });

  it("deletes the current logo and returns the preview to the fallback", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock("/uploads/company/company-1/logo/current.png");
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(window, "confirm").mockReturnValue(true);

    renderPage();

    await user.click(await screen.findByRole("button", { name: /remover logo/i }));

    await waitFor(() => expect(getLogoDeleteCall(fetchMock)).toBeTruthy());
    expect(getLogoDeleteCall(fetchMock)?.[0].toString()).toBe(
      "http://localhost:3001/api/company/logo",
    );
    expect(screen.queryByAltText(/logo atual de jr pinturas/i)).not.toBeInTheDocument();
    expect(screen.getAllByText("JR Pinturas").length).toBeGreaterThan(0);
  });
});
