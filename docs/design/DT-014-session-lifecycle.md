# DT-014 — Session Lifecycle (Fase A: Absolute Session Lifetime)

- **Status:** Implemented (backend apenas — frontend fora de escopo desta fase)
- **Depende de:** DT-011B.2 (JWT/access token), DT-011A.10 (refresh token, rotation, `auth_version`)
- **Escopo desta fase:** apenas o *absolute session lifetime* (8h). Idle timeout, HttpOnly
  cookies e reuse/family detection foram avaliados e explicitamente adiados — ver §3.
- **Nota de numeração:** esta feature foi conduzida internamente como "DT-012 — Session
  Lifecycle", mas o prefixo `DT-012.x` já está em uso neste repositório para um tema
  não relacionado (arquitetura de produção / provisionamento de VPS — ver
  `DT-012.1-production-architecture.md`, `DT-012.4-vps-provisioning.md`,
  `DT-012.4.1-production-readiness-review.md`). Este documento usa `DT-014` (próximo
  número livre após `DT-013-release-pipeline.md`) para evitar colisão. Recomenda-se
  confirmar/ajustar essa numeração antes do commit, se o projeto mantiver um registro
  central de DTs.

---

## 1. Objetivo

Impedir que uma sessão seja prolongada indefinidamente através de refreshes sucessivos.
Antes desta fase, um refresh token só era limitado por:

- sua própria expiração (30 dias, `app.jwt.refresh-token-ttl`);
- revogação explícita (logout, password reset, rotation on-use).

Nada limitava **quanto tempo uma sessão contínua** (login às 08:00, refreshes a cada 15
minutos o dia inteiro) podia durar — na prática, uma sessão ativa podia se estender por
até 30 dias sem jamais exigir novo login. Esta fase fecha esse gap.

## 2. O que foi implementado

### 2.1 Absolute session lifetime (8h, novo)

`refresh_tokens` ganhou a coluna `session_started_at`: o timestamp do login/register/
invite-accept **original** que iniciou a sessão. Esse valor é copiado, sem
recalcular, para cada novo refresh token emitido por uma rotation — nunca é
resetado para "agora".

`AuthService.refresh()` agora rejeita o refresh quando
`now >= sessionStartedAt + absoluteLifetime`, mesmo que o token apresentado ainda
esteja dentro do seu próprio `expiresAt`. A checagem faz parte do mesmo `UPDATE`
atômico que já existia para a rotation (`RefreshTokenRepository
.markRevokedIfStillValid`), preservando a garantia de que duas chamadas concorrentes
com o mesmo token nunca podem ambas vencer.

Configuração: `app.session.absolute-lifetime-seconds` (default `28800` = 8h), env var
`SESSION_ABSOLUTE_LIFETIME_SECONDS`.

### 2.2 Access token 15min — sem alteração de comportamento

`app.jwt.access-token-ttl` continua `900` por default. Única mudança: passou a aceitar
override via env var `ACCESS_TOKEN_TTL` (antes, hardcoded no YAML). Idem para
`app.jwt.refresh-token-ttl` / `REFRESH_TOKEN_TTL` (default `2592000`, inalterado).

### 2.3 Refresh token rotation — sem alteração de comportamento

O modelo atômico existente (revoga o token consumido, emite um novo, tudo em um único
`UPDATE ... WHERE revoked = false AND expires_at > now`) foi apenas **estendido** com
uma terceira condição (`session_started_at > sessionCutoff`) na mesma query — nenhuma
proteção existente foi removida ou enfraquecida.

## 3. O que foi deliberadamente adiado

| Item | Motivo do adiamento |
|---|---|
| **Idle timeout** | Decisão explícita desta fase: não inferir idle a partir de `createdAt`/`session_started_at`, não adicionar `lastActivityAt`. Avaliado em auditoria anterior como barato de adicionar depois (reaproveitando a própria rotation como sinal de atividade), mas fora do escopo aprovado para esta fase. |
| **HttpOnly cookies** | O contrato atual (access + refresh token no corpo JSON) foi mantido integralmente. Migrar para cookie HttpOnly é uma DT de hardening separada, com implicações de CORS/CSRF que merecem escopo próprio. |
| **Session/family model explícito** | Esta fase reaproveita apenas um campo (`session_started_at`) propagado entre rotações — não introduz uma tabela/entidade `Session` nem um `sessionId`/`familyId` formal. Modelagem explícita fica para uma fase futura, se/quando reuse detection for implementado. |
| **Reuse detection com revogação de família** | Ver §3.1 abaixo — motivo específico. |

### 3.1 Por que não usar `revokeAllForUser` como substituto de reuse/family detection

O modelo atual **não possui** o conceito de "família de sessão" — cada rotation gera um
token novo sem nenhum vínculo formal de família além do `session_started_at` que esta
fase acabou de introduzir (que identifica *quando* a sessão começou, não *qual cadeia*
de tokens pertence a ela).

Reuse detection legítimo — no sentido de "um refresh token já rotacionado foi
reapresentado, provável indício de roubo" — deveria revogar **apenas os tokens
descendentes daquela família especificamente comprometida**. `revokeAllForUser`
revoga **todos** os refresh tokens do usuário, de **todas** as sessões/dispositivos
ativos — incluindo sessões legítimas e não relacionadas ao incidente (ex.: o usuário
logado no celular e no desktop simultaneamente). Usar `revokeAllForUser` como reação a
um simples reuse detectado encerraria sessões legítimas do usuário desnecessariamente,
uma consequência desproporcional ao sinal observado (que hoje sequer distingue "reuse
por roubo" de "reuse por race condition benigna do cliente", já que a rejeição de um
token já rotacionado é indistinguível de qualquer outro token inválido). Implementar
isso corretamente requer a modelagem explícita de família (adiada em §3), para que a
revogação em resposta a reuse seja **cirúrgica** — só a família afetada — em vez de
global.

## 4. Migration

`V15__add_session_started_at_to_refresh_tokens.sql` — aditiva, não destrutiva:
adiciona a coluna nullable, faz backfill de todas as linhas existentes a partir de
`created_at`, só então aplica `NOT NULL`. Nenhuma linha de `refresh_tokens` é deletada
ou reescrita destrutivamente. Validado por teste dedicado
(`RefreshTokenSessionStartedAtMigrationTest`) que aplica a migration até `V14`, insere
uma linha no formato pré-`V15`, migra para `V15`, e confirma o backfill correto.

## 5. Contrato HTTP — sem alteração

`POST /auth/refresh` continua retornando a mesma resposta genérica (`422 Unprocessable
Entity`, mensagem uniforme) para qualquer motivo de rejeição — token desconhecido,
expirado, já revogado, ou agora também "sessão excedeu o absolute lifetime". O cliente
não recebe (e não deve receber) nenhum sinal que distinga esses casos.

## 6. Próximos passos sugeridos (fora desta fase)

- **DT-014B** (ou numeração equivalente) — frontend: silent refresh com deduplicação de
  chamadas concorrentes, e o modal global de "sessão expirada".
- Reavaliar idle timeout com a implementação mínima já esboçada (comparar `now` contra
  o `session_started_at`/`createdAt` do token apresentado — sem nova coluna).
- Modelagem explícita de família de sessão, caso reuse detection vire prioridade.
