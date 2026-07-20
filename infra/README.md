# Infra — Contractor Platform (Sprint 11A.1)

Fundação de containers de produção: imagens Docker do backend e do frontend, e um
Docker Compose que sobe o stack completo (frontend + backend + PostgreSQL) localmente,
simulando produção. **Não** cobre provisionamento de VPS, Terraform, deploy por SSH,
publicação no GHCR, backup, Storage Box, systemd ou monitoramento — esses itens são de
etapas posteriores (ver "Próximas etapas" no final deste documento).

---

## Arquitetura dos containers

Arquitetura de produção alvo (definida para o projeto, não totalmente implementada
ainda):

```
Cloudflare
    ↓
Caddy                    ← ainda NÃO implementado nesta sprint (ver Sprint 11A.3)
├── Next.js  (frontend)
└── Spring Boot (backend)
        ↓
    PostgreSQL
```

O que **este** Compose (`infra/compose/docker-compose.prod.yml`) efetivamente sobe:

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  frontend   │     │   backend   │────▶│  postgres    │
│  (Next.js)  │     │(Spring Boot)│     │ (PostgreSQL) │
│  :3000      │     │  :8080      │     │  :5432       │
└─────────────┘     └─────────────┘     └──────────────┘
      │                    │                    │
      └────────────────────┴────────────────────┘
                  rede interna "internal"
```

- **Sem Caddy nesta etapa.** Por isso, `frontend` e `backend` publicam suas portas
  diretamente no host (por padrão `3000`/`8080`, ambas configuráveis via
  `FRONTEND_HOST_PORT`/`BACKEND_PORT` — ver "Rodando em paralelo com outro projeto
  local" abaixo) — uma conveniência **temporária** só para validar o stack
  localmente. Quando o Caddy for adicionado (Sprint 11A.3), ele passa a ser o único
  ponto de entrada público, essas publicações diretas devem ser
  reavaliadas/removidas, e a variável `FRONTEND_HOST_PORT` deixa de ter efeito.
- **`postgres` nunca publica porta no host** — só é alcançável pelos outros
  containers, através da rede `internal`. Essa é a garantia real de isolamento (não
  depende de firewall externo).
- **`frontend` não depende de `backend` no startup** (`depends_on`) — decisão
  deliberada: o frontend precisa continuar subindo mesmo que a API esteja
  temporariamente indisponível (os hooks já tratam estados de erro/carregamento).
- **`backend` aguarda `postgres` ficar saudável** (`depends_on: condition:
  service_healthy`) antes de iniciar.

---

## Pré-requisitos

- Docker Engine com Docker Compose v2 (`docker compose version` deve funcionar —
  **não** use o binário antigo `docker-compose`).
- Para build local das imagens: nenhuma outra ferramenta — o Dockerfile do backend
  baixa o Maven via `mvnw` dentro do próprio build, o do frontend usa `npm ci`.

---

## Como copiar o arquivo de ambiente

```bash
cd infra/env
cp production.env.example production.env
# edite production.env com valores reais — NUNCA faça commit deste arquivo
```

`production.env.example` documenta, variável por variável, para qual propriedade real
da aplicação (Spring `application.yml` / `frontend/src/lib/env/public-env.ts`) cada
uma é mapeada — nenhuma variável foi inventada sem verificar o código correspondente.

---

## Como construir as imagens localmente

A partir de `infra/compose/`:

```bash
cd infra/compose
docker compose --env-file ../env/production.env -f docker-compose.prod.yml build
```

Isso constrói `backend/Dockerfile` e `frontend/Dockerfile` e marca as imagens como
`${BACKEND_IMAGE}:${APP_VERSION}` / `${FRONTEND_IMAGE}:${APP_VERSION}` (por padrão,
`APP_VERSION=local` no `.env.example`). Publicação real no GHCR fica para uma etapa
posterior — nada aqui depende do GHCR para funcionar localmente.

---

## Como iniciar o stack

```bash
cd infra/compose
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d
```

`--env-file` é necessário nos dois lugares: fornece as variáveis que o próprio
`docker compose` interpola (nomes de imagem, versão, credenciais do Postgres) **e**,
via `env_file:` de cada serviço no YAML, as variáveis injetadas dentro dos
containers (`JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, etc.).

### Rodando em paralelo com outro projeto local (porta 3000 já ocupada)

O frontend só publica a porta `3000` por padrão — o lado do host é configurável via
`FRONTEND_HOST_PORT`; o container continua escutando em `3000` internamente em
qualquer caso (isso é o que muda quando o Caddy assume o roteamento na Sprint
11A.3: a publicação direta deixa de existir e `FRONTEND_HOST_PORT` deixa de ser
consultado). Se outra aplicação na máquina já usa `3000`, edite `production.env`:

```bash
cd infra/env
cp production.env.example production.env
# editar production.env e trocar a linha:
#   FRONTEND_HOST_PORT=3000
# por:
#   FRONTEND_HOST_PORT=3001
```

E suba o stack normalmente:

```bash
cd ../compose
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d --build
```

Validar que o frontend respondeu na porta alternativa:

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:3001/
# esperado: HTTP 200
```

---

## Como verificar o estado

```bash
docker compose --env-file ../env/production.env -f docker-compose.prod.yml ps
```

Aguarde a coluna de status mostrar `healthy` para os três serviços (os health checks
têm `start_period` — o backend em particular pode levar até ~45s no primeiro boot).

---

## Como consultar logs

```bash
docker compose -f docker-compose.prod.yml logs -f
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend
docker compose -f docker-compose.prod.yml logs -f postgres
```

Todos os serviços usam o driver `json-file` com rotação (`max-size: 10m`, `max-file:
5`) — logs não crescem sem limite no disco do host.

---

## Como parar os serviços

```bash
docker compose -f docker-compose.prod.yml stop
```

## Como remover containers sem apagar volumes

```bash
docker compose -f docker-compose.prod.yml down
```

`down` (sem `-v`) remove containers e a rede, mas **preserva** os volumes nomeados
(`postgres_data`, `backend_storage`).

## Como apagar volumes conscientemente

```bash
docker compose -f docker-compose.prod.yml down -v
```

**Isto apaga o banco de dados e todos os uploads.** Use apenas quando tiver certeza —
não existe confirmação interativa.

---

## Dados persistentes

| Volume | Caminho no container | Conteúdo | Precisa de backup futuro? |
|---|---|---|---|
| `postgres_data` | `/var/lib/postgresql/data` | Banco de dados completo (todas as tabelas) | **Sim** — dado crítico |
| `backend_storage` | `/app/storage` | Uploads (`app.storage.base-path`): logos e imagens de galeria, servidos via `/uploads/**` | **Sim** — arquivos do cliente, não regeneráveis |

**PDFs de orçamento não são persistidos** — `EstimatePdfService` gera os bytes do PDF
inteiramente em memória a cada requisição (`GET /estimates/{id}/pdf`,
`GET /public/share/{token}/pdf`) e nunca grava em disco. Não existe, portanto, um
terceiro diretório de "arquivos gerados" a versionar ou fazer backup — isto foi
verificado no código (`EstimatePdfService`/`StorageService`), não presumido.

---

## Portas internas vs. publicadas

| Serviço | Porta interna | Publicada no host nesta etapa? |
|---|---|---|
| `postgres` | 5432 | **Não** — nunca |
| `backend` | 8080 | Sim, temporariamente (`${BACKEND_PORT:-8080}`) — até o Caddy existir |
| `frontend` | 3000 | Sim, temporariamente (`${FRONTEND_HOST_PORT:-3000}`) — porta do host configurável, container sempre em 3000; publicação direta some quando o Caddy existir |

---

## Limitações desta etapa (Sprint 11A.1)

- **Sem Caddy** — sem HTTPS, sem reverse proxy, sem Cloudflare na frente. As portas
  3000/8080 ficam diretamente expostas ao host que rodar este Compose.
- **Sem profile Spring dedicado de produção** — decisão deliberada: o
  `application.yml` base já é seguro por padrão (detalhes de erro ocultos, actuator
  restrito); a única coisa que a produção precisa **evitar** é o profile `local`
  (`application-local.yml`, que liga SQL logging e Swagger UI permissivo). Isso já é
  garantido por simplesmente não setar `SPRING_PROFILES_ACTIVE=local`.
- **Frontend sem endpoint de health dedicado** — o health check usa `GET /` como
  substituto temporário, documentado no `frontend/Dockerfile`. Um endpoint dedicado
  (ex. `/api/health`) fica para avaliação futura, se necessário.
- **Sem publicação de imagens no GHCR** — `BACKEND_IMAGE`/`FRONTEND_IMAGE` apontam
  para um caminho GHCR de exemplo, mas nada aqui faz `docker push`; as imagens usadas
  localmente vêm de `docker compose build`.
- **Sem tags imutáveis reais** — `APP_VERSION=local` é o único valor usado até a
  Sprint 11A.2/pipeline de CI passar a gerar tags por commit SHA.
- **Publicação direta de portas do frontend/backend** é temporária, como descrito
  acima — não é o desenho final de rede.

## Próximas etapas (fora desta sprint)

- **Sprint 11A.2:** pipeline de build/publicação de imagens no GHCR por commit SHA;
  possível endpoint de health dedicado no frontend.
- **Sprint 11A.3:** `Caddyfile` real, serviço `caddy` no Compose, TLS via Cloudflare,
  remoção da publicação direta de portas do frontend/backend.
- **Etapas seguintes (não planejadas em detalhe ainda):** provisionamento da VPS
  Hetzner, Terraform, deploy por SSH, backup com Restic + Storage Box, arquivos
  systemd, monitoramento.
