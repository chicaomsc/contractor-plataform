# DT-017C — Team Member Management Backend

- **Status:** Implemented (backend only — frontend untouched)
- **Fase de:** DT-017 — Team & Collaborator Management
- **Depende de:** DT-017A (roles MANAGER/MEMBER + matriz de autorização), DT-017B (convites), DT-011A.10 / DT-014 (`auth_version` + `revokeAllForUser` = invalidação global de sessão), DT-011A.7 (`ActiveAccountFilter`)
- **Escopo desta fase:** gerenciar os *Users que já pertencem à Company* (a "equipe"): listar, trocar o role de um MANAGER/MEMBER, e remover (soft-disable) um MANAGER/MEMBER.

---

## 1. Modelo

Team members **são** os `User` com `companyId = <company do principal>`. Não há
`Membership` — um `User` continua ligado diretamente a uma única `Company` via
`User.companyId`. Nenhuma migration: role change e removal mexem apenas nas colunas já
existentes `users.role`, `users.status` e `users.auth_version`.

Convites pendentes **não** são members — continuam em `GET /team/invitations` (DT-017B).
O frontend combinará os dois blocos.

## 2. Endpoints (todos OWNER-only)

| Método | Path | Efeito |
|---|---|---|
| GET | `/team/members` | Lista todos os `User` tenant da Company (OWNER, MANAGER, MEMBER; qualquer status) |
| PATCH | `/team/members/{userId}/role` | Troca o role de um MANAGER/MEMBER (MANAGER↔MEMBER apenas); invalida as sessões do alvo |
| DELETE | `/team/members/{userId}` | Soft-remove (status → INACTIVE) um MANAGER/MEMBER; invalida as sessões do alvo |

MANAGER / MEMBER / SUPER_ADMIN → **403** em todos os três (`@PreAuthorize("hasRole('OWNER')")`
em `TeamMemberController`, exatamente como `TeamInvitationController`). `companyId` nunca
vem do client — sempre de `JwtPrincipal`.

## 3. Listagem

`userRepository.findByCompanyId(companyId)`, filtrando SUPER_ADMIN (defensivo — nunca
tem `companyId`), ordenado de forma determinística: **OWNER, depois MANAGER, depois
MEMBER**; dentro do grupo, por `name` e depois `email` (case-insensitive). Ordenação
feita em Java sobre a lista pequena de uma Company — mesmo estilo que
`AdminCompanyService` já usa para owners. Um member removido continua na lista com
`status = INACTIVE` (DT-017C §12 — nada é apagado). DTO (`TeamMemberResponse`):
`id, name, email, role, status, createdAt` — **nunca** `passwordHash`, `authVersion`,
ou qualquer dado de refresh token / sessão.

## 4. Proteção do OWNER (service/domain, não só UI)

Nem role change nem removal podem tocar um `User` cujo role seja OWNER (ou SUPER_ADMIN)
— vale inclusive para **outro** OWNER da mesma Company (o fluxo platform-admin pode ter
criado mais de um; isso não é corrigido nem redesenhado aqui). Enforced em **duas
camadas**:

1. Check explícito no service → `ConflictException` (409) com mensagem clara
   ("An OWNER's role cannot be changed / An OWNER cannot be removed through team
   management.").
2. O `WHERE` da UPDATE atômica (`u.role IN (MANAGER, MEMBER)`) — a garantia
   não-burlável, mesmo padrão de duas camadas de `UserRoleInvariant` +
   `chk_users_role_company_id`.

O caller é **sempre** um OWNER (`@PreAuthorize`), então "não pode agir sobre si mesmo"
é consequência de "não pode agir sobre nenhum OWNER" — nenhum check de self separado é
necessário. Transferência de ownership está fora do escopo.

## 5. Role change

`PATCH /team/members/{userId}/role`, body `{ "role": "MANAGER" | "MEMBER" }`. `role` é
String, validado no service (`parseAssignableRole`) → `OWNER`/`SUPER_ADMIN`/lixo →
`BusinessRuleException` (422). Alvo: precisa existir e pertencer à mesma Company
(`findByIdAndCompanyId` — tenant-scoped desde a primeira query; miss ou outra Company →
**404**, sem vazar nome/email/role/status). Alvo OWNER → **409**.

### 6. Idempotência (MANAGER→MANAGER / MEMBER→MEMBER)

O `WHERE` da UPDATE inclui `u.role <> :newRole`, então uma troca sem efeito afeta **0
linhas** → o service **não** incrementa `auth_version` e **não** revoga sessão. Retorna
200 com o estado atual. (Decisão registrada: sucesso silencioso, sem invalidação
desnecessária.)

## 7. Removal

`DELETE /team/members/{userId}` → soft-disable: `status → INACTIVE` (o `UserStatus`
existente — nenhum status novo inventado). **Nunca** delete físico — FKs e histórico em
customers/estimates/services/gallery (`createdBy`/etc.) são preservados. Alvo OWNER →
**409**. Cross-tenant / desconhecido → **404**.

### Idempotência

O `WHERE` da UPDATE inclui `u.status <> INACTIVE`, então remover um member já INACTIVE
afeta 0 linhas → **204 idempotente**, sem re-bump de `auth_version` nem nova revogação.
(Decisão registrada: idempotente, coerente com o `logout` idempotente já existente.)

## 8. Invalidação de sessão (CRÍTICO — DT-017C §7/§10)

Toda mudança **real** (role change que muda o role, ou remoção que desativa) faz, na
**mesma UPDATE atômica**, `auth_version = auth_version + 1`. O incremento é computado no
banco — não é read-modify-write — então concorrência não perde incremento (§22). Logo
em seguida, no **mesmo método `@Transactional`**, `refreshTokenRepository
.revokeAllForUser(userId)`. Resultado:

- access token antigo → rejeitado na **próxima** requisição (`ActiveAccountFilter`
  compara `auth_version`, DT-014) — não espera expirar;
- refresh token antigo → rejeitado (`AuthService.refresh` → `markRevokedIfStillValid`
  retorna 0 → 422);
- após remoção: novo `POST /auth/login` → 401 enquanto INACTIVE
  (`PlatformUserDetails.isEnabled()`).

## 9. Consistência transacional (§8)

`role/status` + `auth_version++` estão numa única UPDATE SQL (atômica por natureza). A
revogação dos refresh tokens roda no mesmo `@Transactional` — se ela falhar, a UPDATE
faz rollback junto: nunca "role mudou mas refresh continuou válido", nunca "refresh
revogado mas role não mudou".

## 10. Concorrência (§22/§23)

- **Duas trocas de role concorrentes:** cada uma é uma UPDATE atômica com
  `auth_version + 1` no banco → sem lost update; o `WHERE ... role IN (MANAGER,MEMBER)`
  reavalia no momento da escrita, então o estado final é sempre um role válido e as
  sessões antigas (que carregam um `auth_version` anterior) são rejeitadas.
- **Remoção competindo com troca de role:** independentemente da ordem, se **alguma**
  remoção rodou, o alvo fica `INACTIVE` com `auth_version` incrementado e refresh
  tokens revogados → `ActiveAccountFilter` rejeita qualquer token antigo (por
  `auth_version` **e** por `status != ACTIVE`), qualquer que seja o role final. Nenhum
  locking pessimista/otimista extra foi necessário; testes de integração em
  `TeamMemberConcurrencyTest` cobrem os dois cenários.

## 11. Company desativada

Sem lógica paralela: `ActiveAccountFilter` já rejeita o token do OWNER quando a Company
está INACTIVE (DT-011A.7 §13), antes de qualquer endpoint `/team/members` ser
alcançado.

## 12. Fora do escopo

Membership; transferência de ownership; correção do "múltiplos OWNERs por Company";
seat limits / plans / billing; multi-company users; MANAGER/MEMBER gerenciando equipe;
mudança de email; criação de OWNER via convite tenant; qualquer alteração semântica em
owner_invites / DT-014 / DT-015 / DT-017A / DT-017B / password recovery / Resend /
platform admin.
