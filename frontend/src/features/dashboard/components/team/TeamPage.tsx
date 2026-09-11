"use client";

import {
  AlertTriangle,
  MailPlus,
  RefreshCw,
  Trash2,
  UserCog,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { getUserRoleLabel } from "@/features/auth/permissions";
import { ApiError } from "@/lib/api/errors";
import {
  useCreateTeamInvitation,
  useRemoveTeamMember,
  useResendTeamInvitation,
  useRevokeTeamInvitation,
  useTeamInvitations,
  useTeamMembers,
  useUpdateTeamMemberRole,
} from "../../hooks/team-hooks";
import {
  createTeamInvitationSchema,
  type CreateTeamInvitationInput,
  type TeamAssignableRole,
  type TeamInvitation,
  type TeamInvitationStatus,
  type TeamMember,
} from "../../types/team";
import { formatDateTime } from "../../utils/forms";
import { zodResolver } from "../../utils/zod-resolver";
import { ErrorState, LoadingState } from "../DashboardState";
import { Field, inputClassName } from "../FormControls";
import { PageHeader } from "../PageHeader";

const statusLabels: Record<string, string> = {
  ACTIVE: "Ativo",
  INACTIVE: "Inativo",
  PENDING: "Pendente",
};

const invitationStatusLabels: Record<TeamInvitationStatus, string> = {
  PENDING: "Pendente",
  EXPIRED: "Expirado",
  USED: "Usado",
  REVOKED: "Cancelado",
};

function getApiErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) {
    return error.body?.detail ?? error.body?.title ?? fallback;
  }

  return fallback;
}

function StatusBadge({ label }: { label: string }) {
  return (
    <span className="inline-flex min-h-7 items-center border border-border bg-background px-3 text-xs font-bold">
      {label}
    </span>
  );
}

function TeamFeedback({
  tone,
  children,
}: {
  tone: "success" | "error";
  children: ReactNode;
}) {
  return (
    <p
      className={
        tone === "success"
          ? "m-0 border border-success bg-background px-4 py-3 text-sm font-semibold text-success"
          : "m-0 border border-error bg-background px-4 py-3 text-sm font-semibold text-error"
      }
      role={tone === "error" ? "alert" : "status"}
    >
      {children}
    </p>
  );
}

function TeamDialog({
  title,
  description,
  children,
  onClose,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDivElement | null>(null);
  const closeButtonRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const previousActiveElement = document.activeElement as HTMLElement | null;
    const focusTarget =
      dialogRef.current?.querySelector<HTMLElement>(
        "input, select, button, textarea, [tabindex]:not([tabindex='-1'])",
      ) ?? dialogRef.current;

    focusTarget?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      previousActiveElement?.focus();
    };
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/45 px-4"
      role="presentation"
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="team-dialog-title"
        aria-describedby={description ? "team-dialog-description" : undefined}
        tabIndex={-1}
        className="max-h-[calc(100vh-2rem)] w-full max-w-lg overflow-y-auto border border-border bg-surface p-6 shadow-sm"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="team-dialog-title"
              className="m-0 font-display text-2xl font-semibold"
            >
              {title}
            </h2>
            {description ? (
              <p
                id="team-dialog-description"
                className="m-0 mt-3 text-sm text-[var(--muted-foreground)]"
              >
                {description}
              </p>
            ) : null}
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            className="border border-border px-3 py-2 text-sm font-semibold hover:border-primary"
            onClick={onClose}
            aria-label="Fechar"
          >
            Fechar
          </button>
        </div>
        <div className="mt-6">{children}</div>
      </div>
    </div>
  );
}

function ConfirmDialog({
  title,
  description,
  confirmLabel,
  isPending,
  onCancel,
  onConfirm,
}: {
  title: string;
  description: string;
  confirmLabel: string;
  isPending: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <TeamDialog title={title} onClose={onCancel}>
      <div className="flex gap-4">
        <AlertTriangle
          size={24}
          className="mt-1 shrink-0 text-error"
          aria-hidden="true"
        />
        <p className="m-0 text-sm text-[var(--muted-foreground)]">
          {description}
        </p>
      </div>
      <div className="mt-8 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <Button
          type="button"
          variant="secondary"
          onClick={onCancel}
          disabled={isPending}
        >
          Cancelar
        </Button>
        <Button type="button" onClick={onConfirm} disabled={isPending}>
          {isPending ? "A processar" : confirmLabel}
        </Button>
      </div>
    </TeamDialog>
  );
}

function InviteDialog({
  isPending,
  error,
  onClose,
  onSubmit,
}: {
  isPending: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (values: CreateTeamInvitationInput) => Promise<void>;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateTeamInvitationInput>({
    resolver: zodResolver(createTeamInvitationSchema),
    defaultValues: { email: "", role: "MEMBER" },
  });

  return (
    <TeamDialog
      title="Convidar colaborador"
      description="O colaborador receberá um convite por e-mail para criar o acesso à empresa."
      onClose={onClose}
    >
      <form
        className="space-y-5"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <Field label="E-mail" error={errors.email}>
          <input
            type="email"
            autoComplete="email"
            className={inputClassName}
            disabled={isPending}
            {...register("email")}
          />
        </Field>
        <Field label="Função" error={errors.role}>
          <select className={inputClassName} disabled={isPending} {...register("role")}>
            <option value="MEMBER">{getUserRoleLabel("MEMBER")}</option>
            <option value="MANAGER">{getUserRoleLabel("MANAGER")}</option>
          </select>
        </Field>

        {error ? <TeamFeedback tone="error">{error}</TeamFeedback> : null}

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={isPending}
          >
            Cancelar
          </Button>
          <Button type="submit" disabled={isPending}>
            <MailPlus size={16} aria-hidden="true" />
            {isPending ? "A criar" : "Enviar convite"}
          </Button>
        </div>
      </form>
    </TeamDialog>
  );
}

function isMutableMember(member: TeamMember) {
  return member.role !== "OWNER" && member.status === "ACTIVE";
}

function nextRoleFor(member: TeamMember): TeamAssignableRole {
  return member.role === "MANAGER" ? "MEMBER" : "MANAGER";
}

function MemberActions({
  member,
  isBusy,
  onChangeRole,
  onRemove,
}: {
  member: TeamMember;
  isBusy: boolean;
  onChangeRole: (member: TeamMember) => void;
  onRemove: (member: TeamMember) => void;
}) {
  if (!isMutableMember(member)) {
    return <span className="text-sm text-[var(--muted-foreground)]">-</span>;
  }

  const nextRole = nextRoleFor(member);

  return (
    <div className="flex flex-wrap gap-2">
      <Button
        type="button"
        variant="ghost"
        size="sm"
        disabled={isBusy}
        onClick={() => onChangeRole(member)}
      >
        <UserCog size={16} aria-hidden="true" />
        Alterar para {getUserRoleLabel(nextRole)}
      </Button>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        disabled={isBusy}
        onClick={() => onRemove(member)}
      >
        <Trash2 size={16} aria-hidden="true" />
        Remover da equipe
      </Button>
    </div>
  );
}

function MembersSection({
  members,
  currentUserId,
  isBusy,
  onChangeRole,
  onRemove,
}: {
  members: TeamMember[];
  currentUserId?: string;
  isBusy: boolean;
  onChangeRole: (member: TeamMember) => void;
  onRemove: (member: TeamMember) => void;
}) {
  return (
    <section className="border border-border bg-surface p-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="m-0 font-display text-2xl font-semibold">Membros</h2>
          <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
            Usuários que pertencem à empresa.
          </p>
        </div>
      </div>

      <div className="mt-6 hidden overflow-x-auto md:block">
        <table className="w-full min-w-[780px] border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-border text-xs uppercase tracking-[0.12em] text-[var(--muted-foreground)]">
              <th className="py-3 pr-4 font-bold">Colaborador</th>
              <th className="px-4 py-3 font-bold">E-mail</th>
              <th className="px-4 py-3 font-bold">Função</th>
              <th className="px-4 py-3 font-bold">Status</th>
              <th className="py-3 pl-4 font-bold">Ações</th>
            </tr>
          </thead>
          <tbody>
            {members.map((member) => (
              <tr key={member.id} className="border-b border-border/70">
                <td className="py-4 pr-4">
                  <p className="m-0 font-semibold">{member.name}</p>
                  {member.id === currentUserId ? (
                    <p className="m-0 mt-1 text-xs font-semibold text-primary">
                      Você
                    </p>
                  ) : null}
                </td>
                <td className="px-4 py-4 text-[var(--muted-foreground)]">
                  {member.email}
                </td>
                <td className="px-4 py-4 font-semibold">
                  {getUserRoleLabel(member.role)}
                </td>
                <td className="px-4 py-4">
                  <StatusBadge label={statusLabels[member.status]} />
                </td>
                <td className="py-4 pl-4">
                  <MemberActions
                    member={member}
                    isBusy={isBusy}
                    onChangeRole={onChangeRole}
                    onRemove={onRemove}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-6 space-y-3 md:hidden">
        {members.map((member) => (
          <article key={member.id} className="border border-border bg-background p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="m-0 font-semibold">{member.name}</p>
                <p className="m-0 mt-1 break-words text-sm text-[var(--muted-foreground)]">
                  {member.email}
                </p>
                {member.id === currentUserId ? (
                  <p className="m-0 mt-1 text-xs font-semibold text-primary">
                    Você
                  </p>
                ) : null}
              </div>
              <StatusBadge label={statusLabels[member.status]} />
            </div>
            <p className="m-0 mt-4 text-sm font-semibold">
              {getUserRoleLabel(member.role)}
            </p>
            <div className="mt-4">
              <MemberActions
                member={member}
                isBusy={isBusy}
                onChangeRole={onChangeRole}
                onRemove={onRemove}
              />
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function canActOnInvitation(invitation: TeamInvitation) {
  return invitation.status === "PENDING";
}

function InvitationsSection({
  invitations,
  isLoading,
  isError,
  isBusy,
  onRetry,
  onResend,
  onRevoke,
}: {
  invitations: TeamInvitation[];
  isLoading: boolean;
  isError: boolean;
  isBusy: boolean;
  onRetry: () => void;
  onResend: (invitation: TeamInvitation) => void;
  onRevoke: (invitation: TeamInvitation) => void;
}) {
  return (
    <section className="border border-border bg-surface p-6">
      <h2 className="m-0 font-display text-2xl font-semibold">
        Convites pendentes
      </h2>
      <p className="m-0 mt-2 text-sm text-[var(--muted-foreground)]">
        Convites criados para colaboradores que ainda precisam aceitar o acesso.
      </p>

      {isLoading ? (
        <div className="mt-6">
          <LoadingState label="A carregar convites" />
        </div>
      ) : null}

      {isError ? (
        <div className="mt-6">
          <ErrorState
            title="Não foi possível carregar os convites"
            description="A listagem consome o endpoint autenticado /team/invitations."
            onRetry={onRetry}
          />
        </div>
      ) : null}

      {!isLoading && !isError && invitations.length === 0 ? (
        <p className="m-0 mt-6 border border-border bg-background p-4 text-sm font-semibold text-[var(--muted-foreground)]">
          Não há convites pendentes.
        </p>
      ) : null}

      {!isLoading && !isError && invitations.length > 0 ? (
        <div className="mt-6 overflow-x-auto">
          <table className="w-full min-w-[760px] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs uppercase tracking-[0.12em] text-[var(--muted-foreground)]">
                <th className="py-3 pr-4 font-bold">E-mail</th>
                <th className="px-4 py-3 font-bold">Função</th>
                <th className="px-4 py-3 font-bold">Status</th>
                <th className="px-4 py-3 font-bold">Enviado em</th>
                <th className="px-4 py-3 font-bold">Expira em</th>
                <th className="py-3 pl-4 font-bold">Ações</th>
              </tr>
            </thead>
            <tbody>
              {invitations.map((invitation) => (
                <tr key={invitation.id} className="border-b border-border/70">
                  <td className="py-4 pr-4 font-semibold">
                    {invitation.email}
                  </td>
                  <td className="px-4 py-4">
                    {getUserRoleLabel(invitation.role)}
                  </td>
                  <td className="px-4 py-4">
                    <StatusBadge
                      label={invitationStatusLabels[invitation.status]}
                    />
                  </td>
                  <td className="px-4 py-4">
                    {formatDateTime(invitation.createdAt)}
                  </td>
                  <td className="px-4 py-4">
                    {formatDateTime(invitation.expiresAt)}
                  </td>
                  <td className="py-4 pl-4">
                    {canActOnInvitation(invitation) ? (
                      <div className="flex flex-wrap gap-2">
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          disabled={isBusy}
                          onClick={() => onResend(invitation)}
                        >
                          <RefreshCw size={16} aria-hidden="true" />
                          Reenviar convite
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          disabled={isBusy}
                          onClick={() => onRevoke(invitation)}
                        >
                          Cancelar convite
                        </Button>
                      </div>
                    ) : (
                      <span className="text-sm text-[var(--muted-foreground)]">
                        -
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

type Confirmation =
  | { type: "role"; member: TeamMember; nextRole: TeamAssignableRole }
  | { type: "remove"; member: TeamMember }
  | { type: "revoke"; invitation: TeamInvitation };

export function TeamPage() {
  const { session } = useAuth();
  const membersQuery = useTeamMembers();
  const invitationsQuery = useTeamInvitations();
  const createInvitationMutation = useCreateTeamInvitation();
  const resendInvitationMutation = useResendTeamInvitation();
  const revokeInvitationMutation = useRevokeTeamInvitation();
  const updateRoleMutation = useUpdateTeamMemberRole();
  const removeMemberMutation = useRemoveTeamMember();
  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [feedback, setFeedback] = useState<{
    tone: "success" | "error";
    message: string;
  } | null>(null);
  const [inviteError, setInviteError] = useState<string | null>(null);

  const members = useMemo(() => membersQuery.data ?? [], [membersQuery.data]);
  const invitations = useMemo(
    () => invitationsQuery.data ?? [],
    [invitationsQuery.data],
  );
  const isMutating =
    createInvitationMutation.isPending ||
    resendInvitationMutation.isPending ||
    revokeInvitationMutation.isPending ||
    updateRoleMutation.isPending ||
    removeMemberMutation.isPending;

  async function submitInvite(values: CreateTeamInvitationInput) {
    setInviteError(null);
    try {
      await createInvitationMutation.mutateAsync(values);
      setIsInviteOpen(false);
      setFeedback({
        tone: "success",
        message: "Convite criado. O envio por e-mail será processado.",
      });
    } catch (error) {
      setInviteError(
        getApiErrorMessage(error, "Não foi possível criar o convite."),
      );
    }
  }

  async function resendInvitation(invitation: TeamInvitation) {
    setFeedback(null);
    try {
      await resendInvitationMutation.mutateAsync(invitation.id);
      setFeedback({ tone: "success", message: "Novo convite criado." });
    } catch (error) {
      setFeedback({
        tone: "error",
        message: getApiErrorMessage(
          error,
          "Não foi possível reenviar o convite.",
        ),
      });
    }
  }

  async function confirmAction() {
    if (!confirmation) {
      return;
    }

    setFeedback(null);

    try {
      if (confirmation.type === "role") {
        await updateRoleMutation.mutateAsync({
          userId: confirmation.member.id,
          role: confirmation.nextRole,
        });
        setFeedback({ tone: "success", message: "Função alterada." });
      }

      if (confirmation.type === "remove") {
        await removeMemberMutation.mutateAsync(confirmation.member.id);
        setFeedback({ tone: "success", message: "Colaborador removido." });
      }

      if (confirmation.type === "revoke") {
        await revokeInvitationMutation.mutateAsync(confirmation.invitation.id);
        setFeedback({ tone: "success", message: "Convite cancelado." });
      }

      setConfirmation(null);
    } catch (error) {
      setFeedback({
        tone: "error",
        message: getApiErrorMessage(error, "Não foi possível concluir a ação."),
      });
    }
  }

  if (membersQuery.isLoading) {
    return <LoadingState label="A carregar equipe" />;
  }

  if (membersQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar a equipe"
        description="A listagem consome o endpoint autenticado /team/members."
        onRetry={() => void membersQuery.refetch()}
      />
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Equipe"
        title="Equipe"
        description="Gerencie os colaboradores e convites da sua empresa."
        action={
          <Button
            type="button"
            onClick={() => {
              setInviteError(null);
              setIsInviteOpen(true);
            }}
          >
            <MailPlus size={16} aria-hidden="true" />
            Convidar colaborador
          </Button>
        }
      />

      {feedback ? (
        <TeamFeedback tone={feedback.tone}>{feedback.message}</TeamFeedback>
      ) : null}

      <MembersSection
        members={members}
        currentUserId={session?.user.id}
        isBusy={isMutating}
        onChangeRole={(member) =>
          setConfirmation({
            type: "role",
            member,
            nextRole: nextRoleFor(member),
          })
        }
        onRemove={(member) => setConfirmation({ type: "remove", member })}
      />

      <InvitationsSection
        invitations={invitations}
        isLoading={invitationsQuery.isLoading}
        isError={invitationsQuery.isError}
        isBusy={isMutating}
        onRetry={() => void invitationsQuery.refetch()}
        onResend={(invitation) => void resendInvitation(invitation)}
        onRevoke={(invitation) =>
          setConfirmation({ type: "revoke", invitation })
        }
      />

      {isInviteOpen ? (
        <InviteDialog
          isPending={createInvitationMutation.isPending}
          error={inviteError}
          onClose={() => setIsInviteOpen(false)}
          onSubmit={submitInvite}
        />
      ) : null}

      {confirmation?.type === "role" ? (
        <ConfirmDialog
          title={`Alterar função de ${confirmation.member.name}?`}
          description={`A pessoa passará de ${getUserRoleLabel(
            confirmation.member.role,
          )} para ${getUserRoleLabel(
            confirmation.nextRole,
          )}. As sessões atuais dela serão encerradas.`}
          confirmLabel="Alterar função"
          isPending={updateRoleMutation.isPending}
          onCancel={() => setConfirmation(null)}
          onConfirm={() => void confirmAction()}
        />
      ) : null}

      {confirmation?.type === "remove" ? (
        <ConfirmDialog
          title={`Remover ${confirmation.member.name} da equipe?`}
          description="O acesso à empresa será encerrado e as sessões atuais serão finalizadas."
          confirmLabel="Remover"
          isPending={removeMemberMutation.isPending}
          onCancel={() => setConfirmation(null)}
          onConfirm={() => void confirmAction()}
        />
      ) : null}

      {confirmation?.type === "revoke" ? (
        <ConfirmDialog
          title="Cancelar este convite?"
          description="Este convite não poderá mais ser usado."
          confirmLabel="Cancelar convite"
          isPending={revokeInvitationMutation.isPending}
          onCancel={() => setConfirmation(null)}
          onConfirm={() => void confirmAction()}
        />
      ) : null}
    </div>
  );
}
