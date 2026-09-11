import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/errors";
import { TeamPage } from "./TeamPage";
import type {
  TeamAssignableRole,
  TeamInvitation,
  TeamMember,
} from "../../types/team";

let members: TeamMember[];
let invitations: TeamInvitation[];
let membersLoading = false;
let membersError = false;
let invitationsLoading = false;
let invitationsError = false;
let createInviteError: unknown = null;
const refetchMembers = vi.fn();
const refetchInvitations = vi.fn();
const createInvitation = vi.fn();
const resendInvitation = vi.fn();
const revokeInvitation = vi.fn();
const updateRole = vi.fn();
const removeMember = vi.fn();

vi.mock("@/features/auth/hooks/auth-context", () => ({
  useAuth: () => ({
    session: {
      user: {
        id: "owner-1",
        companyId: "company-1",
        email: "owner@example.test",
        name: "Francisco",
        role: "OWNER",
        status: "ACTIVE",
      },
      company: {
        id: "company-1",
        name: "JR Pinturas",
        slug: "jr-pinturas",
        email: null,
        country: "PT",
        status: "ACTIVE",
      },
      branding: null,
      settings: null,
    },
  }),
}));

vi.mock("../../hooks/team-hooks", () => ({
  useTeamMembers: () => ({
    data: members,
    isLoading: membersLoading,
    isError: membersError,
    refetch: refetchMembers,
  }),
  useTeamInvitations: () => ({
    data: invitations,
    isLoading: invitationsLoading,
    isError: invitationsError,
    refetch: refetchInvitations,
  }),
  useCreateTeamInvitation: () => ({
    mutateAsync: createInvitation,
    isPending: false,
  }),
  useResendTeamInvitation: () => ({
    mutateAsync: resendInvitation,
    isPending: false,
  }),
  useRevokeTeamInvitation: () => ({
    mutateAsync: revokeInvitation,
    isPending: false,
  }),
  useUpdateTeamMemberRole: () => ({
    mutateAsync: updateRole,
    isPending: false,
  }),
  useRemoveTeamMember: () => ({
    mutateAsync: removeMember,
    isPending: false,
  }),
}));

const now = "2026-09-10T10:00:00Z";

function member(
  id: string,
  name: string,
  role: TeamMember["role"],
  status: TeamMember["status"] = "ACTIVE",
): TeamMember {
  return {
    id,
    name,
    email: `${id}@example.test`,
    role,
    status,
    createdAt: now,
  };
}

function invitation(
  id: string,
  email: string,
  role: TeamAssignableRole,
  status: TeamInvitation["status"] = "PENDING",
): TeamInvitation {
  return {
    id,
    email,
    role,
    status,
    createdAt: now,
    expiresAt: "2026-09-17T10:00:00Z",
  };
}

beforeEach(() => {
  members = [
    member("owner-1", "Francisco", "OWNER"),
    member("manager-1", "João", "MANAGER"),
    member("member-1", "Maria", "MEMBER"),
    member("inactive-1", "Ana", "MEMBER", "INACTIVE"),
  ];
  invitations = [
    invitation("invite-1", "pending@example.test", "MEMBER"),
    invitation("invite-2", "used@example.test", "MANAGER", "USED"),
  ];
  membersLoading = false;
  membersError = false;
  invitationsLoading = false;
  invitationsError = false;
  createInviteError = null;
  vi.clearAllMocks();
  createInvitation.mockImplementation(async (payload) => {
    if (createInviteError) {
      throw createInviteError;
    }

    invitations = [
      invitation("invite-new", payload.email, payload.role),
      ...invitations,
    ];
  });
  updateRole.mockImplementation(async ({ userId, role }) => {
    members = members.map((item) =>
      item.id === userId ? { ...item, role } : item,
    );
  });
  removeMember.mockImplementation(async (userId) => {
    members = members.map((item) =>
      item.id === userId ? { ...item, status: "INACTIVE" } : item,
    );
  });
});

describe("TeamPage", () => {
  it("shows members, friendly labels and protects OWNER/inactive rows from actions", () => {
    render(<TeamPage />);

    expect(screen.getByRole("heading", { name: "Equipe" })).toBeVisible();
    expect(screen.getAllByText("Francisco").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Proprietário").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Administrador").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Colaborador").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Inativo").length).toBeGreaterThan(0);
    expect(
      screen.queryByRole("button", { name: /Alterar Francisco/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Remover Ana/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: /Alterar para Colaborador/ })
        .length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByRole("button", { name: /Remover da equipe/ }).length,
    ).toBeGreaterThan(0);
  });

  it("shows invitations and empty state", () => {
    const { rerender } = render(<TeamPage />);

    expect(screen.getByRole("heading", { name: "Convites pendentes" }));
    expect(screen.getByText("pending@example.test")).toBeInTheDocument();
    expect(screen.getByText("used@example.test")).toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: "Reenviar convite" }).length,
    ).toBe(1);

    invitations = [];
    rerender(<TeamPage />);

    expect(screen.getByText("Não há convites pendentes.")).toBeInTheDocument();
  });

  it("opens invite dialog, validates email and offers only assignable roles", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(
      screen.getByRole("button", { name: "Convidar colaborador" }),
    );

    expect(
      screen.getByRole("dialog", { name: "Convidar colaborador" }),
    ).toBeVisible();
    expect(screen.getByRole("combobox", { name: /Função/ })).toHaveValue(
      "MEMBER",
    );
    expect(screen.getByRole("option", { name: "Colaborador" })).toHaveValue(
      "MEMBER",
    );
    expect(screen.getByRole("option", { name: "Administrador" })).toHaveValue(
      "MANAGER",
    );
    expect(
      screen.queryByRole("option", { name: "Proprietário" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Administrador da plataforma" }),
    ).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Enviar convite" }));
    expect(
      await screen.findByText("Indique o email do colaborador."),
    ).toBeVisible();

    await user.type(
      screen.getByRole("textbox", { name: /E-mail/ }),
      "not-an-email",
    );
    await user.click(screen.getByRole("button", { name: "Enviar convite" }));
    expect(await screen.findByText("Indique um email válido.")).toBeVisible();
  });

  it("submits an invitation, closes the dialog and shows success feedback", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(
      screen.getByRole("button", { name: "Convidar colaborador" }),
    );
    await user.type(
      screen.getByRole("textbox", { name: /E-mail/ }),
      " novo@example.test ",
    );
    await user.selectOptions(
      screen.getByRole("combobox", { name: /Função/ }),
      "MANAGER",
    );
    await user.click(screen.getByRole("button", { name: "Enviar convite" }));

    await waitFor(() =>
      expect(createInvitation).toHaveBeenCalledWith({
        email: "novo@example.test",
        role: "MANAGER",
      }),
    );
    expect(
      screen.queryByRole("dialog", { name: "Convidar colaborador" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText("Convite criado. O envio por e-mail será processado."),
    ).toBeVisible();
  });

  it("keeps the invite dialog open and shows a safe 409 error", async () => {
    const user = userEvent.setup();
    createInviteError = new ApiError(
      "Não foi possível concluir a operação.",
      409,
      { detail: "Já existe um convite pendente para este e-mail nesta empresa." },
    );

    render(<TeamPage />);
    await user.click(
      screen.getByRole("button", { name: "Convidar colaborador" }),
    );
    await user.type(
      screen.getByRole("textbox", { name: /E-mail/ }),
      "pending@example.test",
    );
    await user.click(screen.getByRole("button", { name: "Enviar convite" }));

    expect(
      await screen.findByText(
        "Já existe um convite pendente para este e-mail nesta empresa.",
      ),
    ).toBeVisible();
    expect(screen.getByRole("textbox", { name: /E-mail/ })).toHaveValue(
      "pending@example.test",
    );
  });

  it("changes MANAGER to MEMBER only after confirmation", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(
      screen.getAllByRole("button", { name: "Alterar para Colaborador" })[0],
    );
    expect(
      screen.getByRole("dialog", { name: "Alterar função de João?" }),
    ).toBeVisible();
    await user.click(screen.getByRole("button", { name: "Alterar função" }));

    await waitFor(() =>
      expect(updateRole).toHaveBeenCalledWith({
        userId: "manager-1",
        role: "MEMBER",
      }),
    );
    expect(screen.getByText("Função alterada.")).toBeVisible();
  });

  it("changes MEMBER to MANAGER only after confirmation", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(
      screen.getAllByRole("button", { name: "Alterar para Administrador" })[0],
    );
    await user.click(screen.getByRole("button", { name: "Alterar função" }));

    await waitFor(() =>
      expect(updateRole).toHaveBeenCalledWith({
        userId: "member-1",
        role: "MANAGER",
      }),
    );
  });

  it("removes an active collaborator after confirmation", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(screen.getAllByRole("button", { name: "Remover da equipe" })[0]);
    expect(
      screen.getByRole("dialog", { name: "Remover João da equipe?" }),
    ).toBeVisible();
    await user.click(screen.getByRole("button", { name: "Remover" }));

    await waitFor(() => expect(removeMember).toHaveBeenCalledWith("manager-1"));
    expect(screen.getByText("Colaborador removido.")).toBeVisible();
  });

  it("resends and revokes pending invitations only", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(screen.getByRole("button", { name: "Reenviar convite" }));
    await waitFor(() =>
      expect(resendInvitation).toHaveBeenCalledWith("invite-1"),
    );
    expect(screen.getByText("Novo convite criado.")).toBeVisible();

    await user.click(
      screen.getAllByRole("button", { name: "Cancelar convite" }).at(-1)!,
    );
    expect(
      screen.getByRole("dialog", { name: "Cancelar este convite?" }),
    ).toBeVisible();
    await user.click(
      screen.getAllByRole("button", { name: "Cancelar convite" }).at(-1)!,
    );

    await waitFor(() =>
      expect(revokeInvitation).toHaveBeenCalledWith("invite-1"),
    );
    expect(screen.getByText("Convite cancelado.")).toBeVisible();
  });

  it("closes dialogs with Escape", async () => {
    const user = userEvent.setup();
    render(<TeamPage />);

    await user.click(
      screen.getByRole("button", { name: "Convidar colaborador" }),
    );
    await user.keyboard("{Escape}");

    expect(
      screen.queryByRole("dialog", { name: "Convidar colaborador" }),
    ).not.toBeInTheDocument();
  });

  it("shows independent loading and error states", () => {
    invitationsError = true;
    const { rerender } = render(<TeamPage />);

    expect(
      screen.getByText("Não foi possível carregar os convites"),
    ).toBeInTheDocument();

    membersLoading = true;
    rerender(<TeamPage />);
    expect(screen.getByText("A carregar equipe")).toBeInTheDocument();

    membersLoading = false;
    membersError = true;
    rerender(<TeamPage />);
    expect(screen.getByText("Não foi possível carregar a equipe")).toBeVisible();
  });
});
