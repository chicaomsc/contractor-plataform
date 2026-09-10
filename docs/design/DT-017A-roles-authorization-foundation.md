# DT-017A — Roles & Authorization Foundation

- **Status:** Implemented (backend only — frontend untouched)
- **Fase de:** DT-017 — Team & Collaborator Management
- **Depende de:** DT-011A.7 (SUPER_ADMIN, `ActiveAccountFilter`, multi-tenancy), DT-014 (session lifecycle — `auth_version`/refresh revocation, reaproveitados aqui só como referência, não alterados)
- **Escopo desta fase:** só o modelo de roles e a autorização backend. Nenhuma gestão de equipe (convite, remoção, troca de role via API) foi implementada — ver §5/§6.

---

## 1. Objetivo

Preparar o backend para colaboradores de empresa (`MANAGER`/`MEMBER`) sem implementar a gestão de equipe em si. Dois roles novos, autorização revisada endpoint a endpoint contra uma matriz explícita, e nenhuma mudança de schema.

## 2. Roles

```
SUPER_ADMIN  — administrador da plataforma. companyId = NULL sempre. Nunca é
               tratado como tenant. Escopo: /admin/**.
OWNER        — dono da empresa. companyId != NULL. Controle total do tenant;
               único role que altera perfil/settings da Company; único role
               que gerenciará equipe quando essa feature existir (DT-017B/C).
MANAGER      — administrador/gerente operacional da empresa. companyId != NULL.
               Opera clientes, orçamentos, serviços, galeria, branding. Não
               altera perfil da Company nem settings.
MEMBER       — colaborador operacional. companyId != NULL. Opera clientes e
               orçamentos apenas.
```

Não existe role técnico `ADMIN` — `MANAGER` é o administrador da empresa; `SUPER_ADMIN` continua sendo exclusivamente o administrador da plataforma. Não há helpers ambíguos como `isAdmin()`.

`UserRole` ([auth/domain/UserRole.java](../../backend/src/main/java/io/chicaodw/platform/auth/domain/UserRole.java)): `OWNER`, `SUPER_ADMIN` preservados (ordem e nomes inalterados), `MANAGER`/`MEMBER` acrescentados.

## 3. Modelo de dados — sem mudança estrutural

- **Sem tabela de membership.** Um `User` pertence a exatamente uma `Company` (`company_id != NULL`) — tenant roles — ou a nenhuma (`company_id = NULL`) — só `SUPER_ADMIN`.
- `chk_users_role_company_id` (V11) já aceita qualquer role diferente de `'SUPER_ADMIN'` com `company_id` preenchido — `MANAGER`/`MEMBER` cabem sem alteração. Confirmado por teste real contra Postgres (`UserRoleCompanyConstraintTest.tenantRole_withCompanyId_isAcceptedByDatabaseConstraint_noMigrationNeeded`), não só por leitura do DDL.
- **Nenhuma migration foi criada nesta fase.**

## 4. Matriz de autorização implementada

| Área | OWNER | MANAGER | MEMBER | SUPER_ADMIN |
|---|---|---|---|---|
| Dashboard/API base (`/auth/me`) | YES | YES | YES | N/A (rota de auth, não tenant) |
| Clientes | YES | YES | YES | 403 |
| Orçamentos (+ PDF + share) | YES | YES | YES | 403 |
| Serviços | YES | YES | NO (403) | 403 |
| Galeria | YES | YES | NO (403) | 403 |
| Branding | YES | YES | NO (403) | 403 |
| Empresa — leitura | YES | YES | YES | 403 |
| Empresa — alteração | YES | NO (403) | NO (403) | 403 |
| Settings | YES | NO (403) | NO (403) | 403 |
| Equipe | YES | — | — | — |

"Equipe" ainda não existe como endpoint — a linha documenta apenas a regra futura (só OWNER gerenciará equipe inicialmente), não implementada aqui.

## 5. O que NÃO foi implementado nesta fase (por decisão explícita)

- `TeamController`/`TeamService`/qualquer endpoint de gestão de equipe.
- Convite de colaborador (envio de e-mail, aceite, criação de usuário a partir de convite de equipe).
- Alteração de role via API.
- Remoção de colaborador.
- Transferência de ownership.
- Seat limits / billing / plans.
- Tabela de membership.
- Qualquer alteração em `owner_invites`, recuperação de senha, comportamento do Resend, TTLs de sessão, rotation de refresh, absolute session lifetime ou semântica do platform admin.

## 6. Requisitos documentados para fases futuras

### DT-017B (gestão de equipe)
- Endpoints de convite/listagem/remoção (ver auditoria original, §16 — `GET /team/members`, `POST /team/invitations`, etc., não definitivos).
- Decisão de modelo de convite (reaproveitar o padrão `owner_invites` vs. nova entidade — ambas avaliadas na auditoria, nenhuma escolhida ainda).

### DT-017C (role change / ownership invariants)
- **Troca de role (`MANAGER ↔ MEMBER`) deve invalidar sessões existentes** através do mecanismo já disponível e já testado nesta fase indiretamente (`ActiveAccountFilterTest`): `user.authVersion++` + `refreshTokenRepository.revokeAllForUser(userId)` — exatamente o padrão que `PasswordResetTokenService.resetPassword()` já usa. Sem isso, um access token antigo continuaria carregando um role mais privilegiado até expirar (até 15 min, DT-014). **Não implementado nesta fase.**
- **Invariantes de OWNER, nenhuma implementada ainda:**
  - Company não pode ficar sem OWNER.
  - OWNER não pode remover a si próprio.
  - OWNER não pode se auto-rebaixar.
  - MANAGER não gerencia OWNER (não pode alterar/remover um OWNER).
  - MEMBER não altera roles de ninguém.
  - Um convite de equipe não poderá criar um novo OWNER na primeira versão (`OWNER → MANAGER/MEMBER`, `MANAGER/MEMBER → OWNER` — nenhuma dessas transições existe).
  - Transferência de ownership será feature separada, explícita, não um efeito colateral de troca de role.

## 7. Verificação de compatibilidade

Todos os fluxos existentes preservados sem alteração de comportamento: `register()`/`AdminCompanyService`/`PlatformAdminBootstrapRunner` continuam criando exatamente os roles que criavam antes (`OWNER`/`SUPER_ADMIN`, nunca generalizados). `ActiveAccountFilter`, `UserRoleInvariant`, `AuthService.loadCompanyAndAssertActive` e `PasswordResetTokenService.isEligibleForReset` já eram genéricos (`role == SUPER_ADMIN` como único branch especial) — nenhum deles precisou de alteração de código para reconhecer `MANAGER`/`MEMBER` corretamente; só os `@PreAuthorize` de endpoints de negócio precisaram mudar.
