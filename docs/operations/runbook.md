# Runbook de Produção — Contractor Platform

Última atualização: Sprint 11A.6.

## 1. Objetivo e Escopo

Este é o documento único para operar o Contractor Platform em produção no dia a
dia: subir, parar, atualizar, reverter, verificar saúde, ler logs e agir diante
de um problema — sem precisar reconstruir mentalmente as Sprints 11A.1–11A.5.

**O que este documento não é:** não repete o "porquê" de nenhuma decisão (isso
vive nos Design Técnicos, §3), não repete os comandos detalhados de
backup/restore (isso vive em [infra/backup/README.md](../../infra/backup/README.md)),
e não é uma revisão de segurança de aplicação (isso vive em `docs/security/*.md`).

## 2. Arquitetura Atual

Requisição pública:

```
Internet
  ↓
Caddy (único ponto publicado, ${CADDY_HTTP_PORT:-80})
  ├── "/"        → frontend:3000
  ├── "/api/*"   → backend:8080   (prefixo /api removido antes de encaminhar)
  └── "/uploads/*" → backend:8080 (prefixo mantido)

backend:8080 → postgres:5432 (rede "internal", sem porta pública)
```

Build e publicação de imagem:

```
GitHub (push em main)
  ↓
GitHub Actions (.github/workflows/publish-images.yml)
  ↓
GHCR
  ├── ghcr.io/chicaomsc/contractor-platform-backend:<full-sha|short-sha|main>
  └── ghcr.io/chicaomsc/contractor-platform-frontend:<full-sha|short-sha|main>
  ↓
VPS (docker compose pull / up -d)
```

Backup:

```
PostgreSQL + backend_storage + caddy_data
  ↓
infra/backup/scripts/backup-all.sh
  ↓
Restic (repositório local hoje; Storage Box real na Sprint 11B)
```

Só `caddy` publica porta no host — `frontend`, `backend` e `postgres` só são
alcançáveis pela rede Docker `internal` (confirmado em
`infra/compose/docker-compose.prod.yml`).

## 3. Fontes de Verdade

| Categoria | Onde procurar |
|---|---|
| Decisões técnicas e alternativas rejeitadas | `docs/design/DT-011A.*.md`, `docs/adr/ADR-*.md` |
| Configuração executável (o que roda de fato) | `infra/compose/docker-compose.prod.yml`, `infra/caddy/Caddyfile`, `backend/src/main/resources/application-prod.yml`, `backend/Dockerfile`, `frontend/Dockerfile` |
| Variáveis de ambiente | `infra/env/production.env.example` |
| CI/CD | `.github/workflows/*.yml` |
| Backup/restore/disaster recovery (detalhado) | [infra/backup/README.md](../../infra/backup/README.md) |
| Operação cotidiana | **Este documento** |
| Visão geral de infraestrutura (o quê/por quê) | [infra/README.md](../../infra/README.md) |
| Progresso de produto/negócio | `docs/roadmap.md` |

## 4. Pré-requisitos do Host

Conhecido hoje: Docker Engine + Docker Compose v2 (`docker compose version`, não
o binário antigo `docker-compose`). **A distro/tamanho definitivo da VPS ainda
não foi escolhido** — isso é decisão da Sprint 11B, não presumida aqui.

## 5. Arquivos Locais e Secrets

| Arquivo | Contém | Vai para o Git? |
|---|---|---|
| `infra/env/production.env` | `POSTGRES_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, `NEXT_PUBLIC_*`, `APP_VERSION` | **Não** — gitignored (`infra/env/.gitignore`); copiar de `production.env.example` e preencher |
| `RESTIC_PASSWORD_FILE` (caminho definido em `infra/backup/env/backup.env`) | Senha do repositório Restic | **Não** — precisa também existir fora deste host (password manager), sem isso o backup inteiro fica inacessível |
| Credenciais da Storage Box (Sprint 11B) | Chave SSH para SFTP | **Não** — mesma regra |

Detalhes completos de secrets de backup: [infra/backup/README.md § Secrets](../../infra/backup/README.md#secrets).

## 6. `APP_VERSION` / GHCR

```
ghcr.io/chicaomsc/contractor-platform-backend
ghcr.io/chicaomsc/contractor-platform-frontend
ghcr.io/chicaomsc/contractor-platform-caddy
```

| Tag | Uso |
|---|---|
| `:<full-sha>` (40 chars) | **Referência de deploy — é o valor de `APP_VERSION`** |
| `:<short-sha>` (7 chars) | Alias de conveniência, mesmo digest |
| `:main` | Só inspeção humana no GHCR — nunca usar para deploy |

**Não existe `:latest`** (`flavor: latest=false` explícito em `publish-images.yml`).
Plataforma: **`linux/amd64`** apenas. `APP_VERSION` **deve sempre ser o full SHA**.

**Compatibilidade com o Platform Ops:** `docker-compose.prod.yml` também aceita,
opcionalmente e com prioridade sobre `APP_VERSION`, `BACKEND_VERSION`/
`FRONTEND_VERSION`/`CADDY_VERSION` por componente (mesma regra: full SHA de 40
caracteres). Isso **não muda nada** no fluxo manual documentado neste runbook — se
essas variáveis não forem definidas, o comportamento é idêntico ao de sempre via
`APP_VERSION`. Ver `infra/README.md` § `BACKEND_VERSION`/`FRONTEND_VERSION`/`CADDY_VERSION`.

**GHCR é privado** (Sprint 12.4.2, RR-03) — `docker compose pull` na VPS exige
`docker login ghcr.io` prévio com um PAT `read:packages` (procedimento completo:
`infra/README.md` "Autenticação e permissões"). As três imagens (backend, frontend,
caddy) só são publicadas depois de `backend-ci.yml`/`frontend-ci.yml` passarem —
`publish-images.yml` os chama como workflows reutilizáveis e nunca publica se
qualquer um falhar (RR-08).

**Releases SemVer (Sprint 2C, revisado 2C.1):** além do caminho técnico acima (tags por SHA), o
repositório também publica releases versionadas (`1.2.0`, não só `:<sha>`), automaticamente, a
partir de um merge normal em `main` — via semantic-release, sem Release PR — ver
[CONTRIBUTING.md § Fluxo de release](../../CONTRIBUTING.md#fluxo-de-release) para o processo e
[DT-013](../design/DT-013-release-pipeline.md) para o detalhamento técnico. **O deploy manual
descrito nas seções 11/13 abaixo continua inalterado** — usa `APP_VERSION` (ou, opcionalmente,
`BACKEND_VERSION`/`FRONTEND_VERSION`/`CADDY_VERSION`) com o valor que o operador escolher.

**Promoção automática para produção (Sprint 2C.1):** cada release SemVer publicada aqui abre
automaticamente uma Pull Request no repositório `platform-ops`
(`apps/vantry/production/release.yml`) — a aprovação e o merge dessa PR (fora deste repositório)
é o gate real de produção. Mergear essa PR **não** dispara deploy sozinho — `deploy-production.yml`
(em `platform-ops`) continua exigindo `workflow_dispatch` manual, exatamente como hoje. Requer
`secrets.PLATFORM_OPS_TOKEN` configurado neste repositório — procedimento de setup:
`platform-ops/docs/runbooks/setup-platform-ops-token.md`.

**Estado real hoje (2026-08-23):** `vantry-v1.0.0` existe como tag/GitHub Release, mas nunca foi
promovida — `apps/vantry/production/release.yml` não existe em `platform-ops`, e produção continua
executando o contrato legado `versions.env` (SHA `adbfe3d3451ed372bd55308bbe977dec2d83ed35`). Ver
[DT-013 § Estado real da versão 1.0.0](../design/DT-013-release-pipeline.md#estado-real-da-versão-100--classificação-explícita)
para a classificação completa.

## 7. Startup

```bash
cd infra/compose
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d
```

## 8. Shutdown

```bash
docker compose -f infra/compose/docker-compose.prod.yml stop
```

## 9. Restart

```bash
# Um serviço específico, sem rebuild:
docker compose -f infra/compose/docker-compose.prod.yml restart backend

# Recriar (ex.: depois de mudar uma variável de production.env) — nunca com --build:
docker compose --env-file infra/env/production.env \
  -f infra/compose/docker-compose.prod.yml up -d --force-recreate backend
```

## 10. Status

```bash
docker compose --env-file infra/env/production.env \
  -f infra/compose/docker-compose.prod.yml ps
```

Aguarde a coluna de status mostrar `healthy` nos 4 serviços.

## 11. Deploy

```
merge/push em main
  → .github/workflows/publish-images.yml dispara
  → escolher o full SHA publicado (job "backend"/"frontend", ou git rev-parse HEAD)
  → definir APP_VERSION=<full-sha> em infra/env/production.env
  → docker compose --env-file infra/env/production.env \
      -f infra/compose/docker-compose.prod.yml pull
  → docker compose --env-file infra/env/production.env \
      -f infra/compose/docker-compose.prod.yml up -d
  → health checks (§17)
  → smoke tests (login + listar um dado conhecido)
```

**Nunca use `--build` neste fluxo** — `up -d` sem `--build` nunca reconstrói
localmente quando a imagem referenciada por `image:` já existe (pulled).

## 12. Checklist Pós-Deploy

```
[ ] docker compose ps — os 4 serviços "healthy"
[ ] GET /actuator/health/readiness (backend) → UP
[ ] /api/health (frontend, checado por dentro do container) → 200
[ ] docker inspect --format '{{.State.Health.Status}}' <container_caddy> → healthy
[ ] docker port <caddy> mostra a única porta publicada;
    docker port em backend/frontend/postgres → vazio
[ ] APP_VERSION usado no deploy é o full SHA esperado
[ ] Smoke test: login com usuário conhecido, listar um dado esperado
[ ] Nenhum segredo apareceu em `docker compose logs` durante o deploy
```

## 13. Rollback de Aplicação

```bash
export APP_VERSION=<sha-anterior>
docker compose --env-file infra/env/production.env \
  -f infra/compose/docker-compose.prod.yml pull
docker compose --env-file infra/env/production.env \
  -f infra/compose/docker-compose.prod.yml up -d
```

**Isto reverte só código.** Não toca no banco de dados. Se uma migration
incompatível já rodou, o código antigo pode falhar contra o schema novo — ver §14.

## 14. Flyway — Regra Explícita

**Flyway é forward-only.** Não existe downgrade automático. `rollback de imagem
≠ downgrade de migration`. Se uma migration destrutiva/incompatível já foi
aplicada, rollback de aplicação (§13) **não resolve** — é preciso avaliar um
**restore de banco** ([infra/backup/README.md § Restore de PostgreSQL](../../infra/backup/README.md#restore-de-postgresql)),
aceitando a janela de RPO desde o último snapshot.

## 15. Backup / Restore (resumo)

Cobre PostgreSQL + `backend_storage` + `caddy_data`. **RPO ≤ 24h, RTO ≤ algumas
horas** (premissas técnicas do MVP, não SLA contratual). Retenção: 7 diários +
4 semanais + 6 mensais, por tipo de conteúdo.

```bash
infra/backup/scripts/backup-all.sh                       # backup diário completo
infra/backup/scripts/restore-postgres.sh latest --confirm # restore de banco
infra/backup/scripts/restore-files.sh --target backend-storage latest --confirm
```

**Documentação completa (setup, flags, variáveis, `restic check`, todos os
cenários de restore):** [infra/backup/README.md](../../infra/backup/README.md).
Não duplicado aqui de propósito.

## 16. Disaster Recovery (resumo)

VPS inteira perdida: VPS nova → Docker instalado → `git clone` do repositório →
`production.env` recuperado *de dentro do próprio backup Restic* →
`docker compose pull` (traz as imagens já publicadas no GHCR, sem rebuild) →
restore de PostgreSQL → restore de `backend_storage` → subir Caddy → validações
(§12). Passo a passo completo:
[infra/backup/README.md § Disaster Recovery Total](../../infra/backup/README.md#disaster-recovery-total-vps-nova-hipotética).

## 17. Health Checks

| Serviço | Verificação | Observação |
|---|---|---|
| `backend` | `GET /actuator/health` (agregado), `/actuator/health/liveness` (nunca depende do Postgres), `/actuator/health/readiness` (processo + Postgres — é o que o `HEALTHCHECK` do container usa) | Público (`permitAll` em `SecurityConfig`) |
| `frontend` | `GET /api/health`, checado **de dentro do próprio container** (`http://127.0.0.1:3000/api/health`) | **Nunca roteado externamente pelo Caddy** — ver nota abaixo |
| `postgres` | `pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}` | Padrão do healthcheck do Compose |
| `caddy` | Ver nota abaixo | — |

**Conflito intencional `/api/health`:** externamente, `/api/*` pertence ao
**backend** (Caddy remove o prefixo e encaminha para `backend:8080`, que não
tem rota `/health` — resultaria em 404 do Spring). A rota `/api/health` do
**frontend** só existe e só é chamada **dentro do container do frontend**, pelo
próprio `HEALTHCHECK` do `frontend/Dockerfile` — nunca através do Caddy, nunca
alcançável de fora. Isso é aceito por desenho, não um bug.

**Caddy — atenção:** o `HEALTHCHECK` do container `caddy` verifica a API
administrativa interna (`http://127.0.0.1:2019/config/`), **ligada só ao
loopback dentro do próprio container, nunca publicada** — isso não é um
endpoint público de health, é só o mecanismo interno do Docker para decidir se
o container está `healthy`. Para um operador confirmar Caddy de fora, o
caminho é `docker inspect --format '{{.State.Health.Status}}' <container>` ou
simplesmente `curl http://<host>/` e esperar 200.

## 18. Logs

```bash
docker compose -f infra/compose/docker-compose.prod.yml logs -f [service]
docker logs <container>
```

Todos os 4 serviços usam o driver `json-file` com rotação (`max-size: 10m`,
`max-file: 5`). Caddy loga acesso em JSON para stdout.

**Depois que os templates systemd de backup forem instalados (Sprint 11B):**

```bash
journalctl -u contractor-platform-backup.service
journalctl -u contractor-platform-backup.timer
```

Não disponível ainda hoje — os templates (`infra/backup/systemd/`) existem
versionados mas não estão instalados em nenhum ambiente real.

## 19. Volumes

| Volume | Classificação | Conteúdo |
|---|---|---|
| `postgres_data` | **Negócio** (crítico) | Banco de dados completo |
| `backend_storage` | **Negócio** (crítico) | Uploads (logos, galeria) |
| `caddy_data` | **Operacional** (recomendado, não crítico) | Estado ACME do Caddy — hoje vazio (sem TLS real) |
| `caddy_config` | **Reconstruível** (dispensável) | Config autosalva, 100% derivada do `Caddyfile` versionado |

## 20. Rede / Portas

| Serviço | Porta interna | Publicada no host? |
|---|---|---|
| `postgres` | 5432 | Não |
| `backend` | 8080 | Não |
| `frontend` | 3000 | Não |
| `caddy` | 80 | **Sim** — `${CADDY_HTTP_PORT:-80}`, única porta publicada |

443 está **preparado no Compose mas comentado/inativo** — só será habilitado
quando `CADDY_HOST` apontar para um domínio real (Sprint 11C).

## 21. Caddy

| Caminho externo | Destino | Prefixo |
|---|---|---|
| `/` e demais rotas do app | `frontend:3000` | Repassado como veio |
| `/api/*` | `backend:8080` | Removido antes de encaminhar (`handle_path`) |
| `/uploads/*` | `backend:8080` | Mantido (`handle`) |

**Sprint 12.3:** `/actuator/*` agora recebe um `404` respondido diretamente
pelo próprio Caddy (`handle /actuator/* { respond ... }`), sem proxy para
nenhum upstream — antes caía no catch-all e recebia o 404 do Next.js
(mesmo resultado externo, mas agora explícito/intencional em vez de
incidental). Swagger/OpenAPI continuam sem rota pública, caem no catch-all.

**Mudança operacional (Sprint 11B.6B, `SEC-TENANT-03`):** `/uploads/*` deixou
de ser um resource handler estático do Spring — passou a ser servido por
`PublicUploadController`, que resolve o `companyId` a partir do próprio
caminho (`/uploads/company/{companyId}/...`) e só serve o arquivo se
`Company.status = ACTIVE`. Na prática: **desativar uma empresa (`PATCH
/admin/companies/{id}/status`) agora também bloqueia o logo/galeria dela em
`/uploads/*`** (antes continuavam servíveis indefinidamente); reativar a
empresa restaura o acesso imediatamente, sem nenhuma ação manual sobre os
arquivos em `backend_storage`. Nenhuma mudança de rota no Caddy, cache
(30 dias) ou convenção de path — apenas a autorização por trás de `/uploads/*`
no backend.

### 21.1 Host header e resolução de tenant (SEC-TENANT-04)

`GET /public/tenant` (`PublicTenantController`) resolve o slug do tenant a
partir do header `Host` da própria requisição (`request.getHeader("Host")`) —
nunca de `X-Forwarded-Host` nem de um parâmetro de query, e o backend não lê
`X-Forwarded-Host` em nenhum ponto do código (confirmado em
`DT-011B.3-authorization-multitenancy-review.md` e reafirmado nesta sprint).
Isso significa que **a confiabilidade da resolução de tenant depende
inteiramente do `Host` que chega à borda (Caddy) ser o `Host` real do
cliente**, sem forjamento.

Hoje (`CADDY_HOST=:80`, sem domínio real) o bloco do Caddyfile casa **qualquer**
`Host`, e o `reverse_proxy` repassa o header `Host` do cliente sem alteração —
ou seja, nada na borda valida ainda que o `Host` recebido é legítimo. Isso é
uma limitação arquitetural conhecida e deliberadamente **não fechada ainda**
— o fechamento definitivo depende de `CADDY_HOST` apontar para um domínio
real/wildcard (Sprint 12.3+ vai fechar a config; falta só o domínio real
existir), quando o próprio Caddy passa a rejeitar `Host` fora do domínio
configurado antes mesmo de repassar a requisição ao backend.

**Sprint 12.3:** `trusted_proxies static <ranges Cloudflare>` já foi
adicionado nas opções globais do Caddyfile (ver §21.5) — antecipado porque é
puramente declarativo e **inofensivo enquanto o Cloudflare Proxy não estiver
de fato na frente** (nenhuma conexão chega dessas faixas hoje, então nada
muda na prática); não é o mesmo que "fechar" `SEC-TENANT-04` — isso continua
dependendo do domínio real + `CADDY_HOST` real, não apenas de
`trusted_proxies` estar configurado.

### 21.2 Uploads de imagem — normalização, limites, limpeza e cache (Sprint 11B.6C)

**Formatos aceitos:** PNG, JPEG, WebP (validados por `ImageUploadPolicy` — tamanho,
extensão, assinatura de bytes — e depois de fato decodificados por
`ImageNormalizationService`, que rejeita qualquer arquivo que passe pela validação
superficial mas não seja uma imagem real). Toda imagem aceita é **descartada e
recriada** a partir de um `BufferedImage` decodificado — nunca é uma cópia binária do
upload original — o que remove EXIF/XMP/ICC e aplica a orientação EXIF nos próprios
pixels antes de salvar. PNG e JPEG mantêm o formato original; **WebP é sempre
convertido para PNG** na gravação (o JDK não grava WebP, e não há biblioteca Java
pura mantida que o faça — ver javadoc de `ImageNormalizationService`).

**Limites** (configuráveis via `app.image-normalization.*` / env vars
`IMAGE_MAX_WIDTH_PX` / `IMAGE_MAX_HEIGHT_PX` / `IMAGE_MAX_PIXELS` /
`IMAGE_JPEG_QUALITY`, padrões 6000×6000px / 30 megapixels / qualidade 0.85):
aplicados **antes** de decodificar o buffer de pixels completo (lendo só o
cabeçalho da imagem), especificamente para rejeitar imagens do tipo
"decompression bomb" (arquivo pequeno, dimensões declaradas enormes) sem alocar
memória para elas.

**Limpeza de arquivos substituídos/removidos:** ao trocar um logo ou foto de
galeria, a ordem é sempre gravar o novo arquivo → persistir a nova URL no banco →
só então apagar o arquivo antigo (nunca o inverso); a remoção do arquivo antigo é
best-effort (`StorageService.deleteQuietly`, nunca lança exceção) — uma falha ao
apagar fisicamente o arquivo antigo nunca desfaz ou bloqueia a troca que já foi
persistida. Isso significa que, em caso de falha (raríssima — permissão de
filesystem, disco cheio), um arquivo antigo pode ficar órfão em disco.

**Detecção de órfãos (dry-run apenas):** `GET /admin/storage/orphans`
(SUPER_ADMIN, mesmo padrão de autorização do `AdminController`) lista arquivos em
disco não referenciados por nenhuma linha de `branding`/`gallery_items`. **Não
apaga nada** — é um relatório para um operador revisar manualmente. Para remover
um órfão confirmado: parar o backend (ou aceitar a janela de corrida) e apagar o
arquivo diretamente do volume `backend_storage` pelo path retornado no relatório
(`storedPath`, relativo a `STORAGE_PATH`/`app.storage.base-path`). Não existe
comando de limpeza automática nesta sprint deliberadamente — ver `DT-011B.4 §13`
(`SEC-STORAGE-05`).

**Cache:** `/uploads/**` responde `Cache-Control: no-cache` (não `no-store`, mas
sem validador — ETag/Last-Modified — para revalidar contra, o que na prática força
uma busca nova a cada vez). Substituiu o `max-age` de 30 dias anterior
especificamente para que a desativação de uma empresa (`SEC-TENANT-03`) revogue o
acesso imediatamente, mesmo para quem já tinha a imagem em cache. Estratégia
futura, ainda não implementada: URLs com conteúdo versionado/hash (ex.:
`{uuid}.{hash}.png`) permitiriam voltar a um `max-age` longo com segurança, já que
trocar o arquivo trocaria a própria URL.

**Preparação para storage remoto (S3-compatible):** `StorageService` já é
independente de filesystem — todo conteúdo passa como `byte[]`, todo arquivo é
endereçado por uma storage key (`String`, ex.
`/uploads/company/{id}/logo/{uuid}.png`), nunca um `java.nio.file.Path` ou
`MultipartFile`. Uma implementação S3 precisaria apenas: `store` → `PutObject`;
`load`/`delete`/`deleteQuietly` → `GetObject`/`DeleteObject`; `list` →
`ListObjectsV2` com prefixo. Não implementado nesta sprint (fora de escopo) —
`LocalStorageService` continua sendo a única implementação real.

### 21.3 Autenticação — senha, BCrypt, JWT, refresh, convites (Sprint 11B.6D)

**Política de senha (final):** comprimento 8–128 caracteres, sem regra de
composição (sem exigência de maiúscula/número/símbolo — deliberado, segue NIST
800-63B). Fonte única: `PasswordPolicy` (backend, `auth.domain`) e
`passwordFieldSchema` (frontend, `features/auth/types/auth.ts`) — nunca duplicar
os números 8/128 em um novo formulário/DTO; importar dessas duas fontes. Checagem
contra senhas vazadas (HaveIBeenPwned k-anonymity) **não implementada** — não há
provedor externo integrado; item em aberto (`SEC-AUTH-09`).

**BCrypt:** fator de trabalho configurável via `app.security.bcrypt-strength`
(env `BCRYPT_STRENGTH`), padrão **12** (o padrão do Spring Security é 10). Hashes
existentes continuam validando sem qualquer ação — o BCrypt grava o próprio fator
de trabalho no hash (`$2a$<cost>$...`), então mudar a configuração só afeta
hashes *novos*. **Estratégia futura de rehash** (não implementada): no login
bem-sucedido, extrair o fator de trabalho do hash armazenado (prefixo
`$2a$<cost>$...`) e, se menor que `bcryptStrength` atual, regravar o hash com a
senha em texto plano que o próprio login já tem em mãos naquele momento — migra a
base gradualmente, sem uma migration em massa nem exigir reset de senha de
ninguém. `PasswordEncoder.upgradeEncoding(String)` (interface do Spring
Security) é o ponto de extensão pensado para esse tipo de checagem — validar seu
comportamento exato com `BCryptPasswordEncoder` antes de depender dele.

**JWT — issuer/audience/algoritmo:** todo token emitido carrega `issuer`
(`app.jwt.issuer`, padrão `contractor-platform`) e `audience`
(`app.jwt.audience`, padrão `contractor-platform-api`), exigidos em toda
verificação — um token de outro deployment/ambiente é rejeitado mesmo com
assinatura válida. Algoritmo fixo (`HS256`, explícito na assinatura);
`verifyWith(SecretKey)` restringe a verificação à família HMAC, então um token
assinado com RS/ES/PS, ou não-assinado (`alg: none`), nunca é aceito. Clock skew
configurável via `app.jwt.clock-skew-seconds` (padrão 30s). `ProductionReadinessValidator`
falha o startup em produção se `JWT_ISSUER`/`JWT_AUDIENCE` estiverem em branco,
além das checagens já existentes de `JWT_SECRET` (ausente, placeholder conhecido,
curto demais). **Rotação de chave com `kid` não implementada** — aceitável
enquanto houver um único backend emissor/verificador (`SEC-AUTH-11`); se isso
mudar, reconsiderar RS256/ES256 com chave pública distribuída ou um mapa de
chaves ativas por `kid`.

**Geração e rotação do segredo JWT:** gerar com
`openssl rand -base64 48` (ou equivalente ≥ 32 bytes) — nunca reaproveitar o
placeholder de `infra/env/production.env.example`. Rotação de emergência (suspeita
de vazamento): trocar `JWT_SECRET` e reiniciar a aplicação invalida **todos** os
access tokens emitidos anteriormente de uma vez (não há rotação gradual sem
`kid` — ver acima); refresh tokens não são afetados diretamente pela troca do
segredo (são opacos, validados por hash no banco, não por JWT), mas o próximo
`/auth/refresh` de cada sessão já emitirá um access token com o novo segredo
normalmente.

**Estratégia de refresh token:** opção B — rotação a cada uso (`AuthService.refresh`
sempre revoga o token consumido e emite um novo par). A revogação usa um
`UPDATE` condicional atômico (`RefreshTokenRepository.markRevokedIfStillValid`),
então duas chamadas concorrentes com o mesmo token nunca rotacionam ambas com
sucesso — exatamente uma vence, a outra recebe o mesmo erro de "token
inválido/expirado" que um token genuinamente expirado receberia (nenhum sinal
distinto). Mesmo padrão em `InviteService.acceptInvite`
(`OwnerInviteRepository.markUsedIfStillValid`) para a aceitação de convite.

**Convites — link via fragmento:** `buildInviteLink` gera
`.../invite#token=...` (nunca `?token=...`) — o token nunca aparece em log de
acesso HTTP (Caddy, CDN, proxy) porque um fragmento de URL nunca é enviado ao
servidor. `AcceptInvitePage` lê o token de `window.location.hash` uma única vez,
reescreve a URL imediatamente (`history.replaceState`) e nunca persiste o token
em `localStorage`/`sessionStorage` — mesmo padrão já usado por
`ResetPasswordPage`/`PasswordResetTokenService.buildResetLink`.

**MFA:** não implementado — ver `DT-011B.2` (`SEC-AUTH-12`) para os pontos de
extensão já identificados no código (emissão de token centralizada, `AuthResponse`
não precisa mudar de formato, `UserStatus` já modela "não totalmente autenticado
ainda"). Nenhuma tabela/coluna especulativa foi criada.

**Rate limiting — cobertura ampliada (Sprint 12.4.2, RR-06/RR-07):** além dos cinco
endpoints já limitados (`login`, `forgot-password`, `reset-password`, `invite-accept`,
`admin-password-reset`), `POST /auth/register` (5/hora por IP — o mais restritivo, é
o único endpoint público que cria dado novo sem autenticação) e `POST /auth/refresh`
(20/hora por IP — folgado o bastante para uma sessão ativa trocar o access token a
cada ~15 min, `app.jwt.access-token-ttl`, em múltiplas abas/dispositivos) agora têm
regra própria em `AuthRateLimitFilter`/`RateLimitProperties`. Configurável via
`AUTH_RATE_LIMIT_REGISTER_CAPACITY`/`_WINDOW_SECONDS` e
`AUTH_RATE_LIMIT_REFRESH_CAPACITY`/`_WINDOW_SECONDS`, mesmo padrão dos demais.

**Riscos aceitos restantes desta área (não fechados por decisão explícita):**
- `SEC-AUTH-04` — canal de tempo em `POST /auth/password/forgot` ainda permite
  enumeração por timing, apesar da resposta uniforme. Não corrigido nesta sprint.
- `SEC-AUTH-05` — `POST /auth/register` com e-mail duplicado continua retornando
  um sinal distinto (`409`, mensagem sem o e-mail) — aceito para o MVP B2B
  self-serve atual; reabrir se/quando existir um provedor de e-mail para
  confirmação out-of-band.
- `SEC-AUTH-08` — cookies `contractor_session`/`contractor_role` continuam sendo
  sinais de UX, não de autorização — nunca tratar como fonte de verdade.
- `SEC-AUTH-09` (parcial) — sem checagem de senha vazada.
- `SEC-AUTH-11` (parcial) — sem rotação de chave com `kid`.

### 21.4 Scans automáticos de segurança (Sprint 11B.7)

Política completa, severidade e cadência: `docs/security/security-baseline.md`.
Resumo operacional:

- **Backend (CVE em dependências):** `./mvnw -Psecurity-scan verify -DskipTests -DnvdApiKey=<chave>`
  — requer uma chave gratuita do NVD (<https://nvd.nist.gov/developers/request-an-api-key>),
  sem ela o comando falha rápido (`Invalid API Key`, confirmado nesta sprint — não é
  um modo degradado, é uma falha). Em CI, configurar o secret `NVD_API_KEY`.
- **Frontend (CVE em dependências):** `npm run security:audit` (`frontend/`).
- **Dependências desatualizadas:** `./mvnw versions:display-dependency-updates` /
  `./mvnw versions:display-plugin-updates` (backend); `npm run deps:outdated`
  (frontend).
- **CodeQL** (Java + TypeScript/JavaScript) e **gitleaks** (segredos commitados)
  rodam só em `.github/workflows/security.yml` (agendado semanalmente, manual via
  `workflow_dispatch`, e em todo push para `main`) — nenhum dos dois roda
  localmente como parte do fluxo normal de desenvolvimento.
- **Dependabot** (`.github/dependabot.yml`) abre PRs semanais para Maven/npm/GitHub
  Actions, até 5 simultâneos por ecossistema — tratar como qualquer PR normal.
- Nenhum destes scanners bloqueia `./mvnw test`, `npm run build`, ou os workflows
  `backend-ci.yml`/`frontend-ci.yml` existentes — são inteiramente aditivos.

### 21.5 Domínio, Cloudflare e TLS wildcard via DNS-01 (Sprint 12.3)

Decisão completa: `docs/design/DT-012.1-production-architecture.md §9/§16/ADR-005`.
Resumo operacional do que existe hoje e do que falta para ativar de verdade.

**Imagem Caddy customizada:** `infra/caddy/Dockerfile` builda Caddy via
`xcaddy` com o módulo `github.com/caddy-dns/cloudflare` (pinado em `v0.2.1`,
Caddy pinado em `2.11.4` — `2.8.4` não compila com esse plugin nesta imagem
builder, ver nota de versão no Dockerfile). Substitui a imagem oficial
`caddy:2-alpine` usada até a Sprint 12.2. Build/validação:

```bash
docker build -t contractor-caddy:test infra/caddy
docker run --rm contractor-caddy:test caddy list-modules | grep cloudflare
# esperado: dns.providers.cloudflare

docker run --rm -e CADDY_HOST=":80" \
  -v "$(pwd)/infra/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" \
  contractor-caddy:test caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
```

**Comportamento local vs. produção — mesmo Caddyfile, sem duplicar nada:**
`CADDY_HOST=:80` (local, `production.env.example`) nunca ativa automatic
HTTPS — o bloco `tls { dns cloudflare {env.CLOUDFLARE_API_TOKEN} }` fica
presente na config mas nunca é de fato acionado (sem hostname, não há para
que hostname emitir certificado), então **`CLOUDFLARE_API_TOKEN` não precisa
existir para rodar o compose localmente** — validado nesta sprint subindo o
stack completo sem essa variável definida, todos os 4 serviços `healthy`.
Quando `CADDY_HOST` for `example.com, *.example.com` (produção), o mesmo
bloco passa a emitir/renovar automaticamente um certificado wildcard via
DNS-01.

**Token Cloudflare — regras (reafirmando DT-012.1 §16, não uma decisão nova):**
API Token (nunca a Global API Key), permissão única `Zone:DNS:Edit`, restrita
à zona do domínio real. Nunca no Caddyfile nem em qualquer arquivo versionado
— só em `infra/env/production.env` (gitignored), lido pelo Caddy via
`{env.CLOUDFLARE_API_TOKEN}` (placeholder de runtime do Caddy, não de
Caddyfile — por isso não precisa existir para `caddy validate`/`compose up`
locais).

**Roteiro manual — configurar o domínio no Cloudflare (a fazer quando o
domínio real for adquirido; hoje só documentado, nada executado):**

1. Adicionar o domínio como site no Cloudflare (plano Free já é suficiente).
2. Trocar os nameservers no registrador do domínio para os dois nameservers
   que o Cloudflare atribuir.
3. Esperar o status da zona virar **Active** no painel Cloudflare (propagação
   de nameserver, minutos a ~24h dependendo do registrador/TTL antigo).
4. Criar o registro DNS do apex: `A example.com → <IP do VPS>` (placeholder —
   VPS ainda não provisionada, ver §25 Checklist de Go-Live).
5. Criar o registro wildcard: `A *.example.com → <IP do VPS>` (mesmo IP —
   cobre qualquer subdomínio de tenant sem registro individual).
6. `www`: **não criado** — não há decisão de produto por servir `www.` (ver
   `infra/caddy/Caddyfile`/`production.env.example`); se isso mudar, é só mais
   um registro `A`/`CNAME`, não uma mudança de arquitetura.
7. Modo do proxy Cloudflare (nuvem laranja vs. cinza) para o **primeiro
   deploy**: recomendado começar em **DNS-only (cinza)** — permite validar
   TLS/roteamento direto contra o VPS sem a camada extra do proxy no caminho
   crítico do primeiro go-live; migrar para **Proxied (laranja)** depois de
   confirmar que o site funciona ponta a ponta. Justificativa: isola
   problemas (é o Caddy/VPS que está com defeito, ou é o proxy?) durante a
   janela mais arriscada. Trocar para laranja não exige nenhuma mudança de
   Caddyfile — `trusted_proxies` (já configurado, §21.1) só passa a valer a
   pena assim que o proxy for ligado.

**SSL/TLS mode no painel Cloudflare — obrigatório quando o Proxy estiver
ligado:** **Full (strict)**. Nunca **Flexible** (deixaria o trecho
Cloudflare→VPS em texto plano, mesmo com o navegador vendo HTTPS — anula o
propósito do TLS na origem). "Full (strict)" exige que a origem (Caddy) tenha
um certificado válido — que é exatamente o que o DNS-01 automático já provê,
sem passo manual adicional.

**Próximo passo (fora desta sprint):** VPS real provisionada (Sprint 12.4+
conforme §27 do DT-012.1) — só então o roteiro acima passa de documentação
para execução real, e `CADDY_HOST`/`CLOUDFLARE_API_TOKEN` passam a ter
valores reais em `production.env`.

## 22. Banco de Dados

```bash
# Conectar:
docker compose --env-file infra/env/production.env \
  -f infra/compose/docker-compose.prod.yml exec postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"

# Confirmar migrations aplicadas (dentro do psql):
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
```

Nada além disso — isto não é um manual de PostgreSQL.

## 23. Troubleshooting

| # | Sintoma | Verificação | Ação |
|---|---|---|---|
| 1 | Container aparece `unhealthy` | `docker compose ps`; `docker logs <container>` | Ver linha específica abaixo para o serviço em questão |
| 2 | `backend` nunca fica `healthy`, readiness `DOWN` | `curl .../actuator/health/readiness`; `docker compose ps postgres` | Confirmar Postgres `healthy` primeiro — readiness depende dele por desenho |
| 3 | `postgres` indisponível | `docker compose ps postgres`; `docker logs postgres` | Verificar volume `postgres_data`, espaço em disco, credenciais em `production.env` |
| 4 | Caddy não roteia | `docker compose logs caddy` | Validar sintaxe com a imagem customizada (Sprint 12.3, não `caddy:2-alpine` — falta o módulo `dns cloudflare`): `docker build -t contractor-caddy:test infra/caddy && docker run --rm -e CADDY_HOST=":80" -v "$(pwd)/infra/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" contractor-caddy:test caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` |
| 5 | `frontend` health falhando isoladamente | `docker logs frontend` | Isolado por desenho (sem `depends_on: backend`) — não é sintoma de problema no backend |
| 6 | `docker compose pull` falha / imagem `APP_VERSION` inexistente | Conferir se o SHA existe: `docker manifest inspect ghcr.io/chicaomsc/contractor-platform-backend:<sha>` | Confirmar que `publish-images.yml` de fato publicou aquele SHA (aba Actions/Packages do GitHub) |
| 7 | Migration Flyway falhando no boot | `docker compose logs backend \| grep -i flyway` | Conferir se o schema do banco é compatível com o `APP_VERSION` sendo implantado — ver §14 |
| 8 | Volume ausente (ex. `down -v` acidental) | `docker volume ls` | Restaurar do backup mais recente ([infra/backup/README.md](../../infra/backup/README.md)) — `down -v` não é reversível pelo Compose em si |
| 9 | Backup falhando | `journalctl -u contractor-platform-backup.service` (quando instalado) ou saída direta do script | Exit codes conhecidos: repositório indisponível, senha Restic errada, Postgres indisponível — todos com mensagem clara, sem segredo no log |
| 10 | Porta ocupada localmente (`CADDY_HTTP_PORT`) | `docker compose up` recusa subir / `curl` dá "connection refused" | Trocar `CADDY_HTTP_PORT` em `production.env` |
| 11 | `docker compose up` falha com "variable is required" | Ler qual variável a mensagem cita | `PLATFORM_BASE_DOMAIN`/`PLATFORM_FRONTEND_BASE_URL`/`NEXT_PUBLIC_API_BASE_URL` etc. são obrigatórias mesmo em validação local — ver `infra/env/production.env.example` (Sprint 12.2 adicionou `JWT_ISSUER`/`JWT_AUDIENCE`/`BCRYPT_STRENGTH`/`PLATFORM_FRONTEND_BASE_URL`/rate-limit, que existiam no código desde 11B.6A/D mas nunca tinham sido documentados no `.example`) |
| 12 | Build local não reflete mudança de código | `docker compose ps` mostra o container rodando, mas com comportamento antigo | `docker compose up` reaproveita uma imagem já existente com a mesma tag (`APP_VERSION`) se uma já existir localmente — usar `up -d --build` para forçar rebuild a partir do Dockerfile/contexto atual, como validado na Sprint 12.2 |
| 13 | `docker build infra/caddy` falha em `xcaddy build` com `undefined: zapslog.HandlerOptions` | Ler a versão de `CADDY_VERSION` (ARG) no `infra/caddy/Dockerfile` | Incompatibilidade real entre a versão do Caddy pinada e a versão de `go.uber.org/zap` que o módulo `caddy-dns/cloudflare` traz transitivamente — não é um erro de rede/cache. Confirmado nesta sprint: `2.8.4` falha, `2.11.4` compila. Se subir `CADDY_VERSION` de novo no futuro, testar o build antes de assumir que qualquer tag `2.x` funciona |
| 14 | `docker compose up` pede `CLOUDFLARE_API_TOKEN` mesmo em validação local | — | Não deveria — `CADDY_HOST=:80` nunca aciona o bloco `tls` do Caddyfile (§21.5); se isso acontecer, é sinal de que `CADDY_HOST` foi setado para um domínio real por engano em `production.env` local |

**Comandos de build/execução local (validados na Sprint 12.2)** — ver
`infra/README.md` "Sprint 12.2 — validação local de build/run/persistência" para
o roteiro completo (build, inspeção de imagem, `compose config`, `up -d --build`,
verificação de não-root, teste de persistência via `down`/`up` sem `-v`).
Comandos específicos do Caddy customizado (build, `list-modules`, `caddy
validate`): §21.5, "Sprint 12.3 — imagem Caddy customizada e validação local"
em `infra/README.md`.

## 24. Segurança Operacional

- `production.env` e o arquivo de `RESTIC_PASSWORD_FILE` **nunca são versionados** (gitignored localmente em cada diretório).
- `backend`, `frontend` e `postgres` **não publicam porta** — só `caddy`.
- Segredos nunca aparecem em log (nenhum script ecoa `PGPASSWORD`/`RESTIC_PASSWORD`/credenciais).
- Deploy sempre por `APP_VERSION=<full-sha>` — nunca `:latest` (não existe) e nunca uma tag mutável para produção.
- Backups são criptografados pelo próprio Restic antes de sair do host.

Não é uma nova revisão de segurança — para achados de segurança de aplicação (autenticação, upload, isolamento de tenant), ver `docs/security/*.md`.

## 25. Checklist de Go-Live

### Já validado nas Sprints 11A

```
[x] Containers production-ready (11A.1–11A.3)
[x] Health checks reais respondendo
[x] Roteamento do Caddy (/, /api/*, /uploads/*)
[x] Portas internas confirmadas (só Caddy publica)
[x] GHCR build/push funcionando (11A.4 + estabilização)
[x] Pull público do GHCR confirmado — histórico (11A.4); SUPERSEDED pela decisão de
    GHCR privado (Sprint 12.4.2, RR-03) — ver "Já validado na Sprint 12.4.2" abaixo
[x] linux/amd64 confirmado nas duas imagens
[x] Backup local executado com dados reais (11A.5)
[x] Restore de PostgreSQL validado
[x] Restore de arquivo individual validado
[x] restic check validado
```

### Pendente — Sprint 11B

```
[ ] VPS Hetzner real provisionada
[ ] Storage Box real provisionada
[ ] SFTP testado contra a Storage Box real
[ ] production.env real preenchido no host
[ ] RESTIC_PASSWORD_FILE real gerado e guardado externamente
[ ] Templates systemd de backup instalados e habilitados
[ ] Sizing final de CPU/memória revisado contra o hardware real
```

### Já validado na Sprint 12.3

```
[x] Imagem Caddy customizada (xcaddy + caddy-dns/cloudflare) construída e testada
[x] dns.providers.cloudflare confirmado presente (caddy list-modules)
[x] Caddyfile com tls/dns cloudflare + wildcard + trusted_proxies — caddy validate ok (local :80 e produção simulada)
[x] Validação local sem CLOUDFLARE_API_TOKEN definido — compose up normal, sem ACME
[x] HSTS confirmado ausente em HTTP puro (matcher @https_only)
[x] /actuator/* confirmado bloqueado na borda (404 direto do Caddy)
[x] Roteiro manual de DNS/Cloudflare documentado (§21.5) — não executado (sem domínio real)
```

### Já validado na Sprint 12.4.2 (Pre-Deploy Blocker Remediation)

```
[x] Imagem Caddy customizada agora publicada no GHCR (job novo em publish-images.yml, RR-01)
[x] docker-compose.prod.yml confirmado referenciando exatamente o mesmo nome/tag da imagem publicada
[x] Validação de GitHub Actions Variables (NEXT_PUBLIC_*) reforçada — rejeita CHANGE_ME/
    example.com/.org/.net/localhost/127.0.0.1, não só "vazio" (RR-02)
[x] Documentação de visibilidade do GHCR corrigida para privado — infra/README.md/runbook (RR-03)
[x] ProductionReadinessValidator agora cobre PLATFORM_BASE_DOMAIN/PLATFORM_FRONTEND_BASE_URL/
    DB_PASSWORD contra placeholders (CHANGE_ME, example.com/.org/.net, localhost) (RR-04/RR-05)
[x] Rate limit adicionado a POST /auth/register e POST /auth/refresh (RR-06/RR-07)
[x] publish-images.yml agora depende de backend-ci.yml/frontend-ci.yml passarem (workflow_call, RR-08)
[x] Healthcheck do backend ampliado para 240s totais (90s start_period + 5×30s), documentado (RR-10)
```

### Pendente — Sprint 12.4+ / Go-Live

```
[ ] Domínio real adquirido
[ ] VPS Hetzner real provisionada
[ ] docker login ghcr.io real na VPS com PAT read:packages (RR-03 — mecanismo pronto,
    execução depende da VPS existir)
[ ] DNS configurado no Cloudflare (roteiro §21.5) contra o domínio/VPS reais
[ ] CLOUDFLARE_API_TOKEN real (Zone:DNS:Edit, uma zona) gerado e guardado como secret
[ ] CADDY_HOST=apex,*.dominio real setado em production.env real
[ ] TLS válido emitido via DNS-01 contra a zona real — BLOCKER FOR GO-LIVE
[ ] Cloudflare SSL/TLS mode = Full (strict) confirmado no painel (nunca Flexible)
[ ] HSTS confirmado ativo sobre HTTPS real
[ ] NEXT_PUBLIC_* (GitHub Actions Variables) atualizadas para o domínio real e
    publish-images.yml re-executado antes do primeiro docker compose pull (RR-02)
[ ] IPv6 — decisão ainda em aberto (DT-012.4.1 RR-09), não resolvida nesta sprint
[ ] Backup/restore validados contra Storage Box real (DT-011A.5/DT-012.1, RR-11) — não implementado nesta sprint
[ ] NVD_API_KEY configurada em CI (RR-12) — não implementado nesta sprint
```

Nenhum item das seções "Pendente" deve ser marcado como concluído antes de ser executado de fato contra o ambiente real.
