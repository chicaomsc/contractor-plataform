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
```

| Tag | Uso |
|---|---|
| `:<full-sha>` (40 chars) | **Referência de deploy — é o valor de `APP_VERSION`** |
| `:<short-sha>` (7 chars) | Alias de conveniência, mesmo digest |
| `:main` | Só inspeção humana no GHCR — nunca usar para deploy |

**Não existe `:latest`** (`flavor: latest=false` explícito em `publish-images.yml`).
Plataforma: **`linux/amd64`** apenas. `APP_VERSION` **deve sempre ser o full SHA**.

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

`/actuator/**` e Swagger/OpenAPI **não têm rota pública no Caddy** — caem no
catch-all e recebem o 404 do Next.js, não uma resposta do backend.

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
uma limitação arquitetural conhecida e deliberadamente **não fechada nesta
sprint** (11B.6B) — o fechamento definitivo depende de `CADDY_HOST` apontar
para um domínio real/wildcard (Sprint 11C), quando o próprio Caddy passa a
rejeitar `Host` fora do domínio configurado antes mesmo de repassar a
requisição ao backend. Não introduzir uma lista de proxies confiáveis
(`ForwardedHeaderFilter`/trusted proxies) antes de existir uma borda real
(Cloudflare) na frente do Caddy — fazer isso agora criaria uma falsa sensação
de proteção sem uma borda que de fato a imponha.

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
| 4 | Caddy não roteia | `docker compose logs caddy` | Validar sintaxe: `docker run --rm -v "$(pwd)/infra/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` |
| 5 | `frontend` health falhando isoladamente | `docker logs frontend` | Isolado por desenho (sem `depends_on: backend`) — não é sintoma de problema no backend |
| 6 | `docker compose pull` falha / imagem `APP_VERSION` inexistente | Conferir se o SHA existe: `docker manifest inspect ghcr.io/chicaomsc/contractor-platform-backend:<sha>` | Confirmar que `publish-images.yml` de fato publicou aquele SHA (aba Actions/Packages do GitHub) |
| 7 | Migration Flyway falhando no boot | `docker compose logs backend \| grep -i flyway` | Conferir se o schema do banco é compatível com o `APP_VERSION` sendo implantado — ver §14 |
| 8 | Volume ausente (ex. `down -v` acidental) | `docker volume ls` | Restaurar do backup mais recente ([infra/backup/README.md](../../infra/backup/README.md)) — `down -v` não é reversível pelo Compose em si |
| 9 | Backup falhando | `journalctl -u contractor-platform-backup.service` (quando instalado) ou saída direta do script | Exit codes conhecidos: repositório indisponível, senha Restic errada, Postgres indisponível — todos com mensagem clara, sem segredo no log |
| 10 | Porta ocupada localmente (`CADDY_HTTP_PORT`) | `docker compose up` recusa subir / `curl` dá "connection refused" | Trocar `CADDY_HTTP_PORT` em `production.env` |

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
[x] Pull público do GHCR confirmado (docker pull sem autenticação, backend e frontend)
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

### Pendente — Sprint 11C / Go-Live

```
[ ] Domínio real configurado
[ ] DNS configurado
[ ] Cloudflare configurado
[ ] TLS válido (CADDY_HOST = domínio real) — BLOCKER FOR GO-LIVE
[ ] HSTS habilitado quando apropriado
```

Nenhum item das seções "Pendente" deve ser marcado como concluído antes de ser executado de fato contra o ambiente real.
