# DT-017B — Team Invitations Backend

- **Status:** Implemented (backend only — frontend untouched)
- **Fase de:** DT-017 — Team & Collaborator Management
- **Depende de:** DT-017A (roles MANAGER/MEMBER, matriz de autorização), DT-011A.10 (padrão de token hash-only / consumo atômico / cooldown), DT-012/DT-014 (session lifecycle), infraestrutura de e-mail Resend (DT-011A.10 §e-mail)
- **Escopo desta fase:** só o backend de convites de colaborador. Sem frontend, sem gestão de membros existentes, sem troca de role via API, sem remoção de membro, sem transferência de ownership, sem seat limits/billing, sem tabela de membership.

---

## 1. Por que `team_invitations` é uma entidade separada de `owner_invites`

Dois fluxos de provisionamento distintos:

```
SUPER_ADMIN → owner_invites  → OWNER      (sempre cria uma Company nova)
OWNER       → team_invitations → MANAGER|MEMBER (Company já existe, ganha um colaborador)
```

`owner_invites` sempre aponta para um `User` PENDING **já criado** (o admin cria a
Company + o `User` PENDING, e só então emite o convite). `team_invitations` carrega o
próprio `email` e `role` na linha, porque **no momento do convite não existe `User`
nenhum** para aquela pessoa. Reutilizar `owner_invites` exigiria ou adicionar colunas
`email`/`role` a ela (poluindo um modelo com semântica diferente) ou voltar a criar
`User` PENDING no convite (violando a regra central abaixo). `owner_invites` não foi
tocado — comportamento, schema e endpoints inalterados.

## 2. Regra central: nenhum `User` nasce no convite

Um convite ainda não aceito **não é um `User`**. O `User` (status `ACTIVE`, nunca
`PENDING`) é criado exatamente uma vez, atomicamente, dentro de
`TeamInvitationService.acceptInvitation`. Isso evita linhas de `users` órfãs para
convites abandonados/expirados/typos, um problema latente do fluxo owner-invite.

## 3. Segurança do token

- Gerado com `SecureTokenGenerator` (160 bits, `SecureRandom`, URL-safe) — mesmo
  gerador de `owner_invites`/`password_reset_tokens`.
- Só o `SHA-256` (`TokenHasher.sha256Hex`) é persistido (`token_hash`, `UNIQUE`). O
  valor bruto existe apenas em memória, durante a request de criação, o tempo de
  montar o link e entregá-lo ao `EmailService`.
- **Nunca** retornado em nenhuma resposta de API (`create`/`list`/`resend` devolvem só
  `id/email/role/status/expiresAt/createdAt`). **Nenhum `debugToken`** foi criado —
  ao contrário de `forgotPassword`, esta fase não precisa disso para E2E; se um
  mecanismo local/test vier a ser necessário no futuro, deve seguir o mesmo padrão
  seguro de `ForgotPasswordResponse` (desabilitado em `prod`, validado por
  `ProductionReadinessValidator`).
- **Nunca logado** — nem o token, nem o `token_hash`, nem o link de aceite completo
  (que contém o token), nem a API key do Resend, nem senhas. `ResendEmailService` só
  loga `kind` (`team-invitation`/`password-reset`), status HTTP e nome da classe de
  exceção.

## 4. TTL

`app.team-invitation.ttl-seconds` (env `TEAM_INVITATION_TTL_SECONDS`), default `604800`
(7 dias). Propriedade própria em vez de reusar a constante de 7 dias hardcoded do
owner-invite, para poder divergir sem tocar o outro fluxo. Não é secret, tem default
seguro, não é exigida por `ProductionReadinessValidator`.

## 5. Convite duplicado (Company + email)

Decisão **A** (a preferência registrada no enunciado):

- `POST /team/invitations` **rejeita com 409** se já existe um convite para a mesma
  Company + email que esteja não-usado, não-revogado e não-expirado.
- `POST /team/invitations/{id}/resend` é o mecanismo explícito para substituir: revoga
  o convite antigo (token antigo para de funcionar imediatamente) e emite um novo.

A checagem de duplicidade é um `SELECT`-então-`INSERT` — **não** é atômica contra dois
`POST` genuinamente concorrentes para o mesmo email (ao contrário do aceite, §7, que é
crítico). Um OWNER competindo consigo mesmo aqui é um caso de baixa severidade,
autoinfligido (pior caso: dois convites vivos por alguns instantes, resolvível com um
revoke), não uma fronteira de segurança — por isso **nenhum índice único parcial** foi
adicionado. `users.uq_users_email` continua sendo o backstop real e não-burlável no
aceite.

## 6. E-mail já pertence a um `User`

`users.email` é único globalmente. Um convite para um email que já pertence a
**qualquer** `User` (mesma Company, outra Company, ou um SUPER_ADMIN) é rejeitado com
**409** e a mensagem genérica **"Este e-mail já está associado a uma conta."** — nunca
revelando qual Company, qual role, ou qualquer dado da outra conta. Mesmo colapso já
usado por `AuthService.register` (DT-011B.2 §13).

## 7. Aceite atômico e consistência transacional

`POST /auth/team-invitations/accept` (público — "público" só significa "não exige
JWT"; todas as validações do token continuam obrigatórias):

1. Busca por `token_hash`.
2. **Consumo atômico**: `TeamInvitationRepository.markUsedIfStillValid(id, now)` — o
   mesmo `UPDATE ... WHERE id = :id AND used_at IS NULL AND revoked_at IS NULL AND
   expires_at > :now` de `owner_invites`/`password_reset_tokens`. No máximo um chamador
   concorrente observa retorno `1`.
3. Revalida Company existe + `ACTIVE`.
4. Revalida que o email ainda não pertence a nenhum `User` (janela de corrida entre
   criação e aceite).
5. Cria o `User` (`saveAndFlush`, `try/catch DataIntegrityViolationException` —
   `users.uq_users_email` é o enforcement final).
6. Emite sessão (`AuthResponse`): access token + refresh token com `sessionStartedAt =
   now`, obedecendo integralmente à DT-014 (access TTL, refresh TTL, absolute lifetime,
   rotation, `auth_version`, `ActiveAccountFilter`).

Tudo em **um método `@Transactional`** — se qualquer passo 3–5 lança, a transação
inteira (inclusive o consumo do passo 2) faz rollback, então "convite marcado used +
User não criado" nunca acontece; e o consumo atômico do passo 2 garante que "dois
Users a partir do mesmo token" nunca acontece. Mesmo raciocínio documentado em
`PasswordResetTokenService.resetPassword` / `InviteService.acceptInvite`.

## 8. Comportamento de erro / privacidade

O aceite retorna **sempre a mesma** `BusinessRuleException` (422) com **a mesma
mensagem** — "O convite é inválido ou não está mais disponível." — para token
desconhecido, expirado, usado, revogado, Company inativa, ou email já ocupado. É
deliberadamente **mais uniforme** que `InviteService.acceptInvite` (que ainda devolve
404 para token totalmente desconhecido) — este endpoint público não pode ser usado
como oráculo para nenhum desses casos.

## 9. Falha no envio de e-mail

`EmailService` **nunca lança** (contrato existente). O convite é **persistido antes** da
tentativa de envio; um outage do Resend nunca impede o convite de existir, só de ser
enviado. `resendInvitation` é como o OWNER recupera esse caso (token novo, nova
tentativa de entrega). `TeamInvitationService` ainda tem um `try/catch RuntimeException`
defensivo em volta da chamada, idêntico a `PasswordResetTokenService.sendResetEmail`.

## 10. Revoke / resend

- `DELETE /team/invitations/{id}` — idempotente para um convite já revogado (no-op
  silencioso, 204); **erro de domínio** (409) para um convite já aceito. Cross-tenant:
  404 (padrão anti-IDOR do projeto).
- `POST /team/invitations/{id}/resend` — revoga o antigo (token antigo morre na hora),
  emite novo token + novo prazo + novo e-mail. `invited_by_user_id` do novo convite é
  quem está reenviando agora (mesma semântica de `AdminCompanyService.reissueInvite`).
  409 se já aceito; 404 cross-tenant.

## 11. Autorização

- `/team/invitations` (create/list/revoke/resend) — `@PreAuthorize("hasRole('OWNER')")`.
  MANAGER/MEMBER/SUPER_ADMIN → **403**. `companyId`/`invitedByUserId` vêm
  exclusivamente do `JwtPrincipal`, nunca do body.
- `/auth/team-invitations/accept` — `permitAll` no `SecurityConfig` (adicionado à lista
  existente ao lado de `/auth/invites/accept`).

## 12. Rate limiting

Reusa a infraestrutura `AuthRateLimitFilter` existente (in-memory, por IP+path, só
endpoints de auth). Três chaves novas:

| Chave | Default | Env var |
|---|---|---|
| `team-invitation-create` (`POST /team/invitations`) | 20 / 3600s | `AUTH_RATE_LIMIT_TEAM_INVITATION_CREATE_*` |
| `team-invitation-resend` (`POST /team/invitations/*/resend`) | 10 / 3600s | `AUTH_RATE_LIMIT_TEAM_INVITATION_RESEND_*` |
| `team-invitation-accept` (`POST /auth/team-invitations/accept`) | 10 / 60s | `AUTH_RATE_LIMIT_TEAM_INVITATION_ACCEPT_*` |

`DELETE` não é rate-limitado (o filtro só cobre `POST`, convenção pré-existente).

## 13. Fora de escopo (DT-017C e adiante)

- Listagem completa de membros; troca de role de membro existente (deve disparar
  `auth_version++` + `revokeAllForUser` quando existir — já registrado em DT-017A §6);
  remoção/desativação de membro; transferência de ownership; invariantes de OWNER
  (Company nunca sem OWNER, etc.); seat limits / billing / plans; tabela de membership;
  múltiplas Companies por `User`; MANAGER/MEMBER convidando; convite de OWNER;
  frontend.
