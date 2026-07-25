# Infra — Contractor Platform (Sprints 11A.1 + 11A.2)

Fundação de containers de produção (11A.1): imagens Docker do backend e do frontend, e
um Docker Compose que sobe o stack completo (frontend + backend + PostgreSQL)
localmente, simulando produção. Configuração de produção (11A.2, este documento
atualizado): profile Spring `prod`, shutdown gracioso, health/liveness/readiness,
compressão, CORS validado, storage validado no startup, JVM/CPU/memória, logs. **Não**
cobre Caddy, HTTPS/HSTS, Cloudflare, provisionamento de VPS, Terraform, deploy por SSH,
publicação no GHCR, backup, Storage Box, systemd, monitoramento externo ou cookies
`HttpOnly` para o JWT — esses itens são de etapas posteriores (ver "Próximas etapas" no
final deste documento).

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

## Configuração de Produção (Sprint 11A.2)

Decisão completa e justificada em
[docs/design/DT-011A.2-production-configuration.md](../docs/design/DT-011A.2-production-configuration.md).
Esta seção é o resumo operacional — "o que fazer", não "por que".

### Profile Spring `prod`

`SPRING_PROFILES_ACTIVE=prod` é fixado diretamente no `docker-compose.prod.yml`
(`backend.environment`) — não é uma variável que se define em `production.env`. Isso
ativa `backend/src/main/resources/application-prod.yml`, que soma (não substitui) o
`application.yml` base:

- Shutdown gracioso, compressão HTTP, `forward-headers-strategy`, probes de
  liveness/readiness, timezone UTC do Jackson, níveis de log de produção.
- `ProductionReadinessValidator` — falha o boot (nunca fica `healthy`) se `JWT_SECRET`
  estiver ausente, curto demais, ou for um dos placeholders conhecidos
  (`dev-only-secret-...` do `application.yml`, ou o próprio exemplo do
  `production.env.example`); se `APP_CORS_ALLOWED_ORIGINS` estiver ausente, tiver uma
  entrada em branco, for `*`, não for uma URL http/https válida, ou apontar para
  `localhost`/`127.0.0.1`; ou se `STORAGE_PATH` não existir/não puder ser criado/não for
  gravável. Mensagens de erro nunca incluem o valor do segredo — só o motivo da rejeição.
  **Este validador só age com o profile `prod` ativo** — `./mvnw test`, execução local
  sem Docker e o profile `local` continuam funcionando exatamente como antes.

### Build-time vs. runtime (frontend)

| Categoria | Variáveis | Efeito de mudar o valor |
|---|---|---|
| Build-time (`NEXT_PUBLIC_*`) | `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_COMPANY_SLUG`, `NEXT_PUBLIC_SITE_URL` | **Exige reconstruir a imagem** (`docker compose build frontend` ou `up --build`) — são inlined no bundle JS do browser durante `next build`; reiniciar o container sozinho não muda nada já compilado |
| Runtime | `PORT`, `HOSTNAME`, `NODE_OPTIONS`, `FRONTEND_HOST_PORT` | Basta recriar o container (`up -d`), sem rebuild |
| Fixas na imagem | `NODE_ENV=production` | Não configurável fora de um rebuild do `frontend/Dockerfile` |

Nenhuma variável `NEXT_PUBLIC_*` é ou deve ser um segredo — qualquer pessoa consegue
lê-la no DevTools do browser depois do build.

### Health, liveness e readiness

| Serviço | Endpoint | O que verifica |
|---|---|---|
| `backend` | `GET /actuator/health` | Agregado — visão geral |
| `backend` | `GET /actuator/health/liveness` | Só o processo Java/contexto Spring — **nunca** depende do Postgres |
| `backend` | `GET /actuator/health/readiness` | Processo **+** PostgreSQL acessível — é o que o `healthcheck` do container usa |
| `frontend` | `GET /api/health` | Só o processo Next.js — nunca depende do backend |
| `postgres` | `pg_isready` | Padrão, inalterado |

Todos os quatro endpoints do backend acima são públicos (`permitAll` em
`SecurityConfig`, matcher `/actuator/health` + `/actuator/health/**`) — nenhum outro
caminho de `/actuator/**` foi aberto (ex.: `/actuator/info` continua exigindo
autenticação).

**Trade-off aceito:** como o Docker Compose só tem um sinal de saúde por container, o
`backend` usa `readiness` (não `liveness`) no `HEALTHCHECK` — uma instabilidade
passageira do Postgres pode marcar o container do backend como `unhealthy` mesmo com a
JVM perfeitamente viva. Isso é intencional (ver DT-011A.2 §6.1.6), não um bug.

### Shutdown gracioso

`server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s`
(backend) — o Spring para de aceitar requisições novas e aguarda até 30s as
requisições em voo terminarem antes de o processo sair sozinho.
`stop_grace_period: 40s` no Compose garante que o Docker só manda `SIGKILL` depois
que essa janela do Spring já teve tempo de terminar (40s > 30s, com folga de 10s).

O `frontend` não tem uma fase de drenagem equivalente — o `server.js` standalone
encerra imediatamente ao receber `SIGTERM` (comportamento default do Node, sem
handler customizado). É intencional: esta aplicação não mantém conexões de longa
duração no frontend (sem WebSocket/streaming), então não há nada a drenar.

### JVM

`JAVA_TOOL_OPTIONS` (já definido no Compose, lido automaticamente pela JVM):
`-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError`.
`TZ=UTC` fixado no Compose para logs/timestamps consistentes independente de onde a
VPS estiver. Sem heap dump habilitado por ora (disco de uma VPS pequena, sem
destino/rotação definidos ainda) — um OOM vira reinício limpo do container via
`restart: unless-stopped`, não travamento silencioso.

### Logs

100% stdout/stderr nos três serviços, capturado pelo driver `json-file` do Docker com
rotação (`max-size: 10m`, `max-file: 5`) — nenhum arquivo de log é escrito dentro de
um container. Em produção (`prod` ativo): `io.chicaodw.platform` em `INFO` (não
`DEBUG`), SQL nunca logado, sem cores ANSI, stack traces nunca em resposta HTTP (só no
log do servidor).

### CORS

`APP_CORS_ALLOWED_ORIGINS` — ver comentário detalhado em
`infra/env/production.env.example`. Resumo: obrigatória com `prod` ativo, aceita
múltiplas origens separadas por vírgula, cada uma validada como URL http/https
absoluta, sem espaços residuais, sem wildcard `*`, sem `localhost`/`127.0.0.1`.

### Storage

`STORAGE_PATH` (`/app/storage` no container, volume `backend_storage`) é validado no
startup **apenas com `prod` ativo**: falha o boot se o diretório não existir e não
puder ser criado, ou se existir e não for gravável. Em `local`/test, nenhuma validação
rígida é aplicada — o comportamento antigo (criação sob demanda) continua. A validação
nunca apaga nada — só cria o diretório se ausente e verifica permissões.

### CPU e memória — valores provisórios

```
postgres:  mem_limit 900m  / mem_reservation 500m  / cpus 1.0
backend:   mem_limit 1400m / mem_reservation 900m  / cpus 1.0
frontend:  mem_limit 700m  / mem_reservation 350m  / cpus 0.5
```

**Estes valores são provisórios**, calculados sobre uma premissa não confirmada de VPS
(~2 vCPU/4 GB) — a VPS Hetzner real ainda não foi escolhida. **Serão revisados na
Sprint 11B**, quando o tamanho real da VPS for definido. `mem_limit`/`mem_reservation`
já eram usados desde a 11A.1; `cpus` é novo nesta sprint. Nota técnica: sob
`docker compose up` puro (fora de Swarm), é essa sintaxe legada (`mem_limit`, `cpus`
no nível do serviço) que tem efeito — um bloco `deploy.resources.limits` seria
silenciosamente ignorado sem `docker stack deploy`.

### HSTS, Caddy e JWT `HttpOnly` — fora do escopo desta sprint

- **`Strict-Transport-Security`:** deliberadamente ausente do frontend. Anunciar HSTS
  sobre HTTP puro (sem TLS) não tem efeito garantido e pode ser arriscado se o domínio
  mudar antes do TLS existir. Vira responsabilidade do Caddy na Sprint 11A.3, só depois
  do TLS via Cloudflare estar validado ponta a ponta.
- **Migração do refresh token para cookie `HttpOnly`:** mencionada como melhoria futura
  em `docs/security/authentication-review.md`, permanece fora do escopo — o mecanismo
  atual (access token JWT via header, refresh token opaco) não foi alterado.

### Comandos de validação

```bash
cd infra/compose

# Config válido (variáveis obrigatórias resolvidas, sem erro de sintaxe)
docker compose --env-file ../env/production.env -f docker-compose.prod.yml config

# Subir com rebuild
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d --build

# Estado dos três serviços
docker compose -f docker-compose.prod.yml ps

# Profile ativo do backend
docker compose -f docker-compose.prod.yml logs backend | grep -i "profile"

# Health / liveness / readiness
curl -i http://localhost:${BACKEND_PORT:-8080}/actuator/health
curl -i http://localhost:${BACKEND_PORT:-8080}/actuator/health/liveness
curl -i http://localhost:${BACKEND_PORT:-8080}/actuator/health/readiness

# Readiness cai quando o Postgres cai; liveness não
docker compose -f docker-compose.prod.yml stop postgres
curl -i http://localhost:${BACKEND_PORT:-8080}/actuator/health/liveness    # continua UP
curl -i http://localhost:${BACKEND_PORT:-8080}/actuator/health/readiness  # DOWN
docker compose -f docker-compose.prod.yml start postgres

# Frontend
curl -i http://localhost:${FRONTEND_HOST_PORT:-3000}/api/health

# Headers de segurança do frontend (HSTS deve estar ausente)
curl -sD - -o /dev/null http://localhost:${FRONTEND_HOST_PORT:-3000}/ \
  | grep -iE "content-security-policy|x-content-type-options|x-frame-options|referrer-policy|strict-transport-security"

# CORS permitida vs. rejeitada
curl -si -X OPTIONS http://localhost:${BACKEND_PORT:-8080}/auth/login \
  -H "Origin: https://SEU_DOMINIO_CONFIGURADO" -H "Access-Control-Request-Method: POST" \
  | grep -i access-control-allow-origin
curl -si -X OPTIONS http://localhost:${BACKEND_PORT:-8080}/auth/login \
  -H "Origin: https://origem-nao-permitida.example" -H "Access-Control-Request-Method: POST" \
  | grep -i access-control-allow-origin   # esperado: sem correspondência

# Outros endpoints do Actuator continuam protegidos
curl -i http://localhost:${BACKEND_PORT:-8080}/actuator/info   # esperado: 401

# Limites de memória
docker stats --no-stream $(docker compose -f docker-compose.prod.yml ps -q)

# Persistência: upload sobrevive a down/up
docker compose -f docker-compose.prod.yml down
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d
```

### Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| `backend` sai (`Exited`) logo após subir, sem ficar `healthy` | `ProductionReadinessValidator` rejeitou a configuração | `docker compose logs backend` — a mensagem indica exatamente qual variável (nunca o valor do segredo) |
| `backend` nunca fica `healthy`, mas não sai | `/actuator/health/readiness` respondendo `DOWN` | Verificar se o Postgres está `healthy` primeiro (`docker compose ps postgres`) |
| CORS falha no browser mesmo com a origem "certa" | Espaço/protocolo/porta diferente do configurado, ou variável não redefinida (ainda no exemplo) | Conferir `APP_CORS_ALLOWED_ORIGINS` no `production.env` real, não no `.example` |
| Mudei `NEXT_PUBLIC_API_BASE_URL` e nada mudou | Variável build-time — precisa de rebuild | `docker compose build frontend` (ou `up --build`), não só `restart` |
| `docker compose up` recusa subir citando uma variável `is required` | Falta uma variável obrigatória em `production.env` (`POSTGRES_*`, `NEXT_PUBLIC_*`) | Comparar com `production.env.example` |

## Limitações desta etapa (Sprints 11A.1 + 11A.2)

- **Sem Caddy** — sem HTTPS, sem reverse proxy, sem Cloudflare na frente. As portas
  3000/8080 ficam diretamente expostas ao host que rodar este Compose.
- **Sem HSTS** — deliberado enquanto não há TLS; vira responsabilidade do Caddy na
  Sprint 11A.3 (ver seção acima).
- **CPU/memória provisórios** — calculados sobre uma premissa de VPS (~2 vCPU/4 GB)
  ainda não confirmada; serão revisados na Sprint 11B quando a VPS Hetzner real for
  escolhida.
- **Sem publicação de imagens no GHCR** — `BACKEND_IMAGE`/`FRONTEND_IMAGE` apontam
  para um caminho GHCR de exemplo, mas nada aqui faz `docker push`; as imagens usadas
  localmente vêm de `docker compose build`.
- **Sem tags imutáveis reais** — `APP_VERSION=local` é o único valor usado até um
  pipeline de CI passar a gerar tags por commit SHA (Sprint 11A.4).
- **Publicação direta de portas do frontend/backend** é temporária, como descrito
  acima — não é o desenho final de rede.
- **Sem migração do JWT/refresh token para cookie `HttpOnly`** — mecanismo de
  autenticação inalterado nesta sprint (ver seção acima).

## Próximas etapas (fora desta sprint)

- **Sprint 11A.3:** `Caddyfile` real, serviço `caddy` no Compose, TLS via Cloudflare,
  HSTS, remoção da publicação direta de portas do frontend/backend.
- **Sprint 11A.4:** pipeline de build/publicação de imagens no GHCR por commit SHA.
- **Sprint 11A.5:** backup (Restic, Storage Box).
- **Sprint 11A.6:** consolidação final da documentação operacional.
- **Sprint 11B:** provisionamento real da VPS Hetzner, Terraform (se adotado), e
  revisão dos valores provisórios de CPU/memória contra o hardware real.
- **Etapas seguintes (não planejadas em detalhe ainda):** deploy por SSH,
  monitoramento externo, observabilidade completa.
