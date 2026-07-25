# Infra — Contractor Platform (Sprints 11A.1 + 11A.2 + 11A.3)

Fundação de containers de produção (11A.1): imagens Docker do backend e do frontend, e
um Docker Compose que sobe o stack completo (frontend + backend + PostgreSQL)
localmente, simulando produção. Configuração de produção (11A.2): profile Spring
`prod`, shutdown gracioso, health/liveness/readiness, compressão, CORS validado,
storage validado no startup, JVM/CPU/memória, logs. Reverse proxy (11A.3, este
documento atualizado): Caddy como único ponto de entrada público — roteamento de `/`,
`/api/*` e `/uploads/*`, compressão, persistência e healthcheck do próprio Caddy;
`frontend`/`backend`/`postgres` deixam de publicar porta no host. **Não** cobre
HTTPS real, HSTS, Cloudflare, domínio real, provisionamento de VPS, Terraform, deploy
por SSH, publicação no GHCR, backup, Storage Box, systemd, monitoramento externo ou
cookies `HttpOnly` para o JWT — esses itens são de etapas posteriores (ver "Próximas
etapas" no final deste documento).

---

## Arquitetura dos containers

Arquitetura de produção alvo (definida para o projeto; Caddy já implementado nesta
sprint, Cloudflare/domínio real ainda não):

```
Cloudflare               ← ainda NÃO implementado (domínio real, DNS, TLS gerenciado)
    ↓
Caddy                    ← implementado nesta sprint (Sprint 11A.3)
├── Next.js  (frontend)
└── Spring Boot (backend)
        ↓
    PostgreSQL
```

O que **este** Compose (`infra/compose/docker-compose.prod.yml`) efetivamente sobe:

```
                          ┌───────────┐
        host: :80 ───────▶   caddy   │
                          └─────┬─────┘
              ┌──────────────────┼──────────────────┐
              │ "/" , /_next/*   │ /api/*            │ /uploads/*
              ▼                  ▼ (prefixo removido)▼ (prefixo mantido)
      ┌─────────────┐     ┌─────────────┐     ┌──────────────┐
      │  frontend   │     │   backend   │────▶│  postgres    │
      │  (Next.js)  │     │(Spring Boot)│     │ (PostgreSQL) │
      │  :3000      │     │  :8080      │     │  :5432       │
      └─────────────┘     └─────────────┘     └──────────────┘
            │                    │                    │
            └────────────────────┴────────────────────┘
                        rede interna "internal"
```

- **Só o `caddy` publica porta no host** (por padrão `80`, configurável via
  `CADDY_HTTP_PORT` — ver "Rodando em paralelo com outro projeto local" abaixo).
  `frontend`, `backend` e `postgres` são alcançáveis **apenas** pela rede Docker
  `internal` (`frontend:3000`, `backend:8080`) — nenhum dos três publica porta no
  host. Isso é o desenho final de rede desta camada; ver "Roteamento (Sprint
  11A.3)" abaixo para o detalhe de cada regra.
- **`postgres` nunca publica porta no host** — só é alcançável pelos outros
  containers, através da rede `internal`. Essa é a garantia real de isolamento (não
  depende de firewall externo).
- **`frontend` não depende de `backend` no startup** (`depends_on`) — decisão
  deliberada: o frontend precisa continuar subindo mesmo que a API esteja
  temporariamente indisponível (os hooks já tratam estados de erro/carregamento).
- **`backend` aguarda `postgres` ficar saudável** (`depends_on: condition:
  service_healthy`) antes de iniciar.
- **`caddy` aguarda `frontend` e `backend` ficarem saudáveis** (`depends_on:
  condition: service_healthy` nos dois) — só para ordem de startup, não para o
  próprio healthcheck do Caddy: o healthcheck do `caddy` nunca chama frontend ou
  backend (ver "Healthcheck do Caddy" abaixo), então uma instabilidade de qualquer
  um dos dois nunca marca o Caddy como `unhealthy`.

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

### Rodando em paralelo com outro projeto local (porta 80 já ocupada)

Desde a Sprint 11A.3, só o `caddy` publica porta no host — por padrão `80`, via
`CADDY_HTTP_PORT`. `frontend` e `backend` nunca publicam porta própria; mudar
`CADDY_HTTP_PORT` é a única coisa necessária para liberar o stack inteiro em outra
porta. Se outra aplicação na máquina já usa `80`, edite `production.env`:

```bash
cd infra/env
cp production.env.example production.env
# editar production.env e adicionar/trocar a linha:
#   CADDY_HTTP_PORT=8000
```

E suba o stack normalmente:

```bash
cd ../compose
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d --build
```

Validar que o Caddy respondeu na porta alternativa (roteando para o frontend):

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8000/
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
docker compose -f docker-compose.prod.yml logs -f caddy
```

Todos os serviços usam o driver `json-file` com rotação (`max-size: 10m`, `max-file:
5`) — logs não crescem sem limite no disco do host. O `caddy` loga em JSON para
stdout (`log { output stdout; format json }` no Caddyfile) — cada linha é um acesso,
sem nenhum segredo (`JWT_SECRET`, senhas, tokens) neles; só método, caminho,
status, duração e alguns cabeçalhos.

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
(`postgres_data`, `backend_storage`, `caddy_data`, `caddy_config`).

## Como apagar volumes conscientemente

```bash
docker compose -f docker-compose.prod.yml down -v
```

**Isto apaga o banco de dados, todos os uploads e o estado interno do Caddy
(`caddy_data`/`caddy_config` — inclui certificados TLS, quando existirem).** Use
apenas quando tiver certeza — não existe confirmação interativa.

---

## Dados persistentes

| Volume | Caminho no container | Conteúdo | Precisa de backup futuro? |
|---|---|---|---|
| `postgres_data` | `/var/lib/postgresql/data` | Banco de dados completo (todas as tabelas) | **Sim** — dado crítico |
| `backend_storage` | `/app/storage` | Uploads (`app.storage.base-path`): logos e imagens de galeria, servidos via `/uploads/**` | **Sim** — arquivos do cliente, não regeneráveis |
| `caddy_data` | `/data` | Estado persistente do Caddy — certificados/chaves TLS e metadados de renovação ACME (nada gravado ali enquanto `CADDY_HOST=:80`, já que não há automatic HTTPS a persistir) | Ainda não crítico nesta sprint (sem domínio real); passa a ser quando o ACME real for ativado |
| `caddy_config` | `/config` | Config ativa do Caddy autosalva (JSON adaptado a partir do Caddyfile) — permite reiniciar sem reprocessar o Caddyfile do zero | Não — é derivada do `Caddyfile`, que já está versionado em `infra/caddy/` |

**PDFs de orçamento não são persistidos** — `EstimatePdfService` gera os bytes do PDF
inteiramente em memória a cada requisição (`GET /estimates/{id}/pdf`,
`GET /public/share/{token}/pdf`) e nunca grava em disco. Não existe, portanto, um
terceiro diretório de "arquivos gerados" a versionar ou fazer backup — isto foi
verificado no código (`EstimatePdfService`/`StorageService`), não presumido.

---

## Portas internas vs. publicadas

| Serviço | Porta interna | Publicada no host? |
|---|---|---|
| `postgres` | 5432 | **Não** — nunca |
| `backend` | 8080 | **Não** — só alcançável via `backend:8080` na rede `internal` (o Caddy fala com ela) |
| `frontend` | 3000 | **Não** — só alcançável via `frontend:3000` na rede `internal` (o Caddy fala com ela) |
| `caddy` | 80 (443 preparado, não publicado ainda) | **Sim** — `${CADDY_HTTP_PORT:-80}:80`, o único ponto de entrada público desta stack |

Confirmar isso na prática: `docker compose ps` só deve mostrar uma entrada em `PORTS`
para o `caddy`; `docker port <container_backend>`, `docker port <container_frontend>`
e `docker port <container_postgres>` devem retornar vazio.

---

## Roteamento (Sprint 11A.3)

Config real em [infra/caddy/Caddyfile](caddy/Caddyfile); decisão completa em
[docs/design/DT-011A.3-caddy-reverse-proxy.md](../docs/design/DT-011A.3-caddy-reverse-proxy.md).
Esta seção é o resumo operacional.

| Caminho externo | Destino | Prefixo mantido? |
|---|---|---|
| `/` e qualquer caminho não listado abaixo (`/dashboard/**`, `/login`, `/share/[token]`, `/_next/*`, etc.) | `frontend:3000` | — (repassado como veio) |
| `/api/*` | `backend:8080` | **Não** — `/api` é removido antes de encaminhar (`handle_path`). Os controllers Spring nunca foram renomeados para incluir `/api` (`/auth`, `/estimates`, `/customers`, `/company`, `/branding`, `/settings`, `/services`, `/gallery`, `/public/sites`, `/public/share`) — o prefixo existe **só** nesta borda do Caddy |
| `/uploads/*` | `backend:8080` | **Sim** — encaminhado tal como veio (`handle`, sem `handle_path`); o backend já serve exatamente `/uploads/**` (`StorageWebConfig`), então não há prefixo a remover |

**`/actuator/**` e Swagger/OpenAPI UI não têm rota pública no Caddy** — não existe
nenhum `handle`/`handle_path` para eles. Os healthchecks Docker do próprio backend
continuam chamando `http://127.0.0.1:8080/actuator/health/...` diretamente dentro do
container, sem passar pelo Caddy nem pela rede `internal`. Externamente, qualquer
tentativa de acessar `/actuator/**` ou `/swagger-ui` através do Caddy cai no `handle`
catch-all e recebe o 404 padrão do Next.js — não há erro custom para isso, é
simplesmente "esta rota não existe" do ponto de vista de quem está fora.

### Nota: `/api/health` do frontend

O Next.js tem sua própria rota interna `GET /api/health` (usada só pelo healthcheck
Docker do container `frontend`, chamada diretamente como
`http://localhost:3000/api/health` **dentro** do próprio container — nunca através da
rede `internal` nem do Caddy). Essa rota colide, em nome, com a regra pública
`/api/* → backend`: externamente, uma requisição a `/api/health` passa pelo
`handle_path /api/*` do Caddy e vai para o **backend** (que não tem esse endpoint —
resultaria em 404 do Spring), nunca chega ao Next.js. Isso é **aceitável por
desenho**: nada externo precisa chamar o `/api/health` do frontend, e o healthcheck
Docker do frontend nunca depende do Caddy para funcionar. Não foi criada nenhuma
exceção no Caddy para expor essa rota publicamente — nem deveria.

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
| Runtime | `PORT`, `HOSTNAME`, `NODE_OPTIONS` | Basta recriar o container (`up -d`), sem rebuild |
| Fixas na imagem | `NODE_ENV=production` | Não configurável fora de um rebuild do `frontend/Dockerfile` |

Nenhuma variável `NEXT_PUBLIC_*` é ou deve ser um segredo — qualquer pessoa consegue
lê-la no DevTools do browser depois do build.

**Sprint 11A.3 — `NEXT_PUBLIC_API_BASE_URL` mudou de significado.** Antes do Caddy,
apontava direto para a porta publicada do backend (`http://localhost:8080` ou um
domínio `api.*` próprio). Agora deve ser o **mesmo** valor de `NEXT_PUBLIC_SITE_URL`
(a mesma origem pública que o Caddy serve) — o frontend adiciona o prefixo `/api`
internamente (`frontend/src/lib/api/api-path.ts`, `withApiPrefix`, aplicado nos 4
pontos que montam uma URL de API) antes de chamar `new URL(path,
NEXT_PUBLIC_API_BASE_URL)`. É o Caddy (`handle_path /api/*`) que remove esse prefixo
antes de encaminhar ao backend — os controllers Spring continuam sem `/api`. Uma
tentativa de usar `NEXT_PUBLIC_API_BASE_URL=/api` (caminho relativo, sem origem) foi
testada e rejeitada — tanto o `new URL(path, base)` do WHATWG quanto o schema Zod que
valida essa variável (`z.string().url()`) exigem uma URL absoluta; ver
docs/design/DT-011A.3-caddy-reverse-proxy.md §9 para a verificação.

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
absoluta, sem espaços residuais, sem wildcard `*`, sem `localhost`/`127.0.0.1`. A
lógica de validação e o filtro CORS em si não mudaram nesta sprint.

**Sprint 11A.3 — CORS deixou de ser a única linha de defesa.** Com Caddy servindo
frontend e backend na mesma origem pública, chamadas do browser a `/api/*` são
same-origin e nunca disparam preflight/CORS. `APP_CORS_ALLOWED_ORIGINS` continua
obrigatória e continua validada da mesma forma — é defesa em profundidade para
chamadas de outra origem (clientes não-browser, uma futura divisão por subdomínio),
não algo de que a topologia atual dependa para funcionar.

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

## Caddy / Reverse Proxy (Sprint 11A.3)

Decisão completa em
[docs/design/DT-011A.3-caddy-reverse-proxy.md](../docs/design/DT-011A.3-caddy-reverse-proxy.md).
Config real em [infra/caddy/Caddyfile](caddy/Caddyfile) — layout único, usado tanto
localmente quanto (sem mudança nenhuma) numa futura implantação com domínio real.
Roteamento em si já coberto acima ("Roteamento (Sprint 11A.3)").

### HTTP local hoje, HTTPS real mais tarde — mesmo arquivo

`{$CADDY_HOST}` no topo do Caddyfile é o único ponto variável:

- `CADDY_HOST=:80` (valor usado nesta sprint, `production.env.example`) — Caddy serve
  HTTP puro em todas as interfaces e **nunca tenta ACME/HTTPS automático**. Não é
  configurado nenhum domínio real.
- `CADDY_HOST=app.exemplo.com` (futuro, Sprint 11C+) — Caddy passa a provisionar e
  renovar automaticamente um certificado TLS real via ACME para esse domínio. Nenhuma
  outra linha do Caddyfile muda.

Nem Cloudflare, nem DNS, nem um domínio real são configurados nesta sprint — a porta
80 crua é suficiente para validar todo o roteamento, compressão e persistência.

### Compressão

`encode zstd gzip` no Caddyfile — negociada por `Accept-Encoding` do cliente (zstd
preferido quando o cliente aceita, senão gzip). Isso é **além**, não em vez, da
compressão HTTP que Spring (`server.compression.enabled`) e Next.js já aplicam por
conta própria — nenhum dos dois foi removido ou alterado nesta sprint; nenhum tuning
adicional foi feito.

### Headers de segurança — Caddy não duplica nada

`Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`/
`frame-ancestors`, `Referrer-Policy` e `Permissions-Policy` continuam sendo
responsabilidade exclusiva do Next.js e do Spring — o Caddyfile não define nenhum
desses headers. Isso significa que os headers precisam ser validados **através** do
Caddy (não só direto no `frontend:3000`/`backend:8080`, que não são mais alcançáveis
de fora) para confirmar que o proxy os repassa sem alterar — ver "Comandos de
validação" abaixo.

### Persistência do Caddy

`caddy_data` (`/data`) e `caddy_config` (`/config`) são volumes nomeados — sobrevivem
a `docker compose down` (sem `-v`) e a recriações do container. Ver a tabela "Dados
persistentes" acima.

### Segurança do container

`read_only: true` na raiz do sistema de arquivos do container; `/tmp` recebe um
`tmpfs` próprio (onde o Caddy grava temporários/escreve atomicamente); `/data` e
`/config` continuam graváveis como volumes nomeados. Nenhuma imagem customizada foi
criada só para rodar non-root, e nenhum `cap_add` foi adicionado — a imagem oficial
`caddy:2-alpine` roda como veio, sem hardening adicional fora deste escopo.

### Healthcheck do Caddy

```yaml
test: ["CMD", "wget", "-q", "-O", "/dev/null", "http://127.0.0.1:2019/config/"]
```

Verifica a API administrativa do próprio Caddy (`127.0.0.1:2019`, só acessível de
dentro do container, nunca publicada) — **não** o site block público, e portanto
nunca depende do `frontend` ou do `backend` estarem no ar. Uma instabilidade em
qualquer um dos dois nunca marca o `caddy` como `unhealthy`. `depends_on` com
`condition: service_healthy` nos dois upstreams existe só para ordem de startup (ver
diagrama de arquitetura acima), não para o healthcheck em si.

### HSTS, TLS real e JWT `HttpOnly` — ainda fora do escopo

- **`Strict-Transport-Security`:** ainda ausente, deliberadamente — não configurado no
  Caddyfile nesta sprint (`CADDY_HOST=:80`, sem TLS real). Anunciar HSTS sobre HTTP
  puro não tem efeito garantido e pode ser arriscado se o domínio mudar antes do TLS
  existir. Fica para quando um domínio real + TLS via Cloudflare estiverem validados
  ponta a ponta (Sprint 11C+).
- **Cloudflare, DNS, domínio real:** não configurados nesta sprint — ver seção acima.
- **Migração do refresh token para cookie `HttpOnly`:** mencionada como melhoria futura
  em `docs/security/authentication-review.md`, permanece fora do escopo — o mecanismo
  atual (access token JWT via header, refresh token opaco) não foi alterado.

### Comandos de validação

Todos os comandos abaixo assumem `CADDY_HOST=:80`/`CADDY_HTTP_PORT=80` (os defaults
de `production.env.example`) — troque `80` se você mudou `CADDY_HTTP_PORT`.

```bash
cd infra/compose

# Config válido (variáveis obrigatórias resolvidas, sem erro de sintaxe)
docker compose --env-file ../env/production.env -f docker-compose.prod.yml config

# Subir com rebuild
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d --build

# Estado dos quatro serviços — aguardar "healthy" em todos
docker compose -f docker-compose.prod.yml ps

# Profile ativo do backend
docker compose -f docker-compose.prod.yml logs backend | grep -i "profile"

# ── Evidência de que só o Caddy publica porta ──────────────────────────────
docker compose -f docker-compose.prod.yml ps
# esperado: coluna PORTS preenchida só na linha do "caddy"
docker port "$(docker compose -f docker-compose.prod.yml ps -q backend)"   # esperado: vazio
docker port "$(docker compose -f docker-compose.prod.yml ps -q frontend)"  # esperado: vazio
docker port "$(docker compose -f docker-compose.prod.yml ps -q postgres)"  # esperado: vazio
docker port "$(docker compose -f docker-compose.prod.yml ps -q caddy)"     # esperado: 80/tcp -> 0.0.0.0:80

# ── Healthcheck do próprio Caddy (API admin, não o site block) ─────────────
docker inspect --format '{{.State.Health.Status}}' "$(docker compose -f docker-compose.prod.yml ps -q caddy)"
# esperado: healthy

# ── Roteamento ──────────────────────────────────────────────────────────────
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/                       # esperado: 200 (frontend)
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/api/company             # esperado: 200/401 do backend, não 404 do Next.js
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/uploads/qualquer-arquivo-existente.png
                                                                                        # esperado: 200 (se o arquivo existir) — nunca prefixado com /api

# /actuator/health e /swagger-ui não devem ser alcançáveis publicamente pelo Caddy
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/actuator/health   # esperado: 404 do Next.js (catch-all), não 200 do Spring
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/swagger-ui/index.html
                                                                                  # esperado: 404 do Next.js

# Healthchecks internos do Docker continuam via rede Docker / dentro do container, não pelo Caddy:
docker compose -f docker-compose.prod.yml exec backend wget -qO- http://127.0.0.1:8080/actuator/health/readiness
docker compose -f docker-compose.prod.yml exec frontend wget -qO- http://127.0.0.1:3000/api/health

# ── Headers de segurança — validados ATRAVÉS do Caddy, não direto no Next.js/Spring ──
curl -sD - -o /dev/null http://localhost/ \
  | grep -iE "content-security-policy|x-content-type-options|x-frame-options|referrer-policy|strict-transport-security"
# esperado: os quatro primeiros presentes (vindos do Next.js), strict-transport-security AUSENTE

# ── Compressão do Caddy ──────────────────────────────────────────────────────
curl -sD - -o /dev/null -H "Accept-Encoding: zstd, gzip" http://localhost/ | grep -i content-encoding
curl -sD - -o /dev/null -H "Accept-Encoding: gzip" http://localhost/api/company | grep -i content-encoding

# ── CORS permitida vs. rejeitada (backstop — same-origin já não depende disto) ──
curl -si -X OPTIONS http://localhost/api/auth/login \
  -H "Origin: https://SEU_DOMINIO_CONFIGURADO" -H "Access-Control-Request-Method: POST" \
  | grep -i access-control-allow-origin
curl -si -X OPTIONS http://localhost/api/auth/login \
  -H "Origin: https://origem-nao-permitida.example" -H "Access-Control-Request-Method: POST" \
  | grep -i access-control-allow-origin   # esperado: sem correspondência

# ── Limites de memória ──────────────────────────────────────────────────────
docker stats --no-stream $(docker compose -f docker-compose.prod.yml ps -q)

# ── Persistência: uploads e volumes do Caddy sobrevivem a down/up (NÃO down -v) ──
docker compose -f docker-compose.prod.yml down
docker compose --env-file ../env/production.env -f docker-compose.prod.yml up -d
docker volume ls | grep -E "caddy_data|caddy_config|backend_storage|postgres_data"   # esperado: todos ainda existem
```

### Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| `backend` sai (`Exited`) logo após subir, sem ficar `healthy` | `ProductionReadinessValidator` rejeitou a configuração | `docker compose logs backend` — a mensagem indica exatamente qual variável (nunca o valor do segredo) |
| `backend` nunca fica `healthy`, mas não sai | `/actuator/health/readiness` respondendo `DOWN` | Verificar se o Postgres está `healthy` primeiro (`docker compose ps postgres`) |
| CORS falha no browser mesmo com a origem "certa" | Espaço/protocolo/porta diferente do configurado, ou variável não redefinida (ainda no exemplo) | Conferir `APP_CORS_ALLOWED_ORIGINS` no `production.env` real, não no `.example` |
| Mudei `NEXT_PUBLIC_API_BASE_URL` e nada mudou | Variável build-time — precisa de rebuild | `docker compose build frontend` (ou `up --build`), não só `restart` |
| `docker compose up` recusa subir citando uma variável `is required` | Falta uma variável obrigatória em `production.env` (`POSTGRES_*`, `NEXT_PUBLIC_*`) | Comparar com `production.env.example` |
| `caddy` nunca fica `healthy` | Config inválida no `Caddyfile`, ou porta 2019 (admin API) inacessível dentro do container | `docker compose logs caddy`; validar sintaxe com `docker run --rm -v "$(pwd)/../caddy/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` |
| `GET /api/algumacoisa` retorna 404 do Next.js em vez de resposta do backend | Rota não bate com `handle_path /api/*`, ou backend não está healthy | Conferir se o caminho tem exatamente o prefixo `/api/`; `docker compose ps backend` |
| `/uploads/...` retorna 404 mesmo com o arquivo existindo | Caminho não corresponde ao que o backend realmente serve, ou volume `backend_storage` vazio (down -v acidental) | Conferir `resolveAdminAssetUrl`/`resolvePublicAssetUrl` no frontend; `docker compose exec backend ls /app/storage` |
| `curl http://localhost/` dá "connection refused" | `CADDY_HTTP_PORT` diferente do usado no curl, ou `caddy` não subiu | `docker compose ps caddy`; conferir `CADDY_HTTP_PORT` em `production.env` |

## Limitações desta etapa (Sprints 11A.1 + 11A.2 + 11A.3)

- **Sem Cloudflare, sem domínio real, sem TLS/HTTPS real** — `CADDY_HOST=:80` serve
  HTTP puro; o Caddyfile já suporta um domínio real sem alteração (ver "HTTP local
  hoje, HTTPS real mais tarde" acima), mas isso não foi configurado nem testado nesta
  sprint.
- **Sem HSTS** — deliberado enquanto não há TLS real (ver seção acima).
- **CPU/memória provisórios** — calculados sobre uma premissa de VPS (~2 vCPU/4 GB)
  ainda não confirmada; serão revisados na Sprint 11B quando a VPS Hetzner real for
  escolhida — inclui os limites do próprio `caddy`, adicionados nesta sprint com a
  mesma premissa provisória.
- **Sem publicação de imagens no GHCR** — `BACKEND_IMAGE`/`FRONTEND_IMAGE` apontam
  para um caminho GHCR de exemplo, mas nada aqui faz `docker push`; as imagens usadas
  localmente vêm de `docker compose build`.
- **Sem tags imutáveis reais** — `APP_VERSION=local` é o único valor usado até um
  pipeline de CI passar a gerar tags por commit SHA (Sprint 11A.4).
- **Sem migração do JWT/refresh token para cookie `HttpOnly`** — mecanismo de
  autenticação inalterado nesta sprint (ver seção acima).
- **Sem hardening adicional do container Caddy** além de `read_only`/`tmpfs` — sem
  imagem customizada non-root, sem `cap_add`, sem revisão de superfície além do que
  já vem na imagem oficial.

## Próximas etapas (fora desta sprint)

- **Sprint 11A.4:** pipeline de build/publicação de imagens no GHCR por commit SHA.
- **Sprint 11A.5:** backup (Restic, Storage Box).
- **Sprint 11A.6:** consolidação final da documentação operacional.
- **Sprint 11B:** provisionamento real da VPS Hetzner, Terraform (se adotado), e
  revisão dos valores provisórios de CPU/memória (incluindo o `caddy`) contra o
  hardware real.
- **Sprint 11C+ (não planejada em detalhe ainda):** domínio real, DNS/Cloudflare, TLS
  automático via ACME (só trocar `CADDY_HOST`), HSTS.
- **Etapas seguintes (não planejadas em detalhe ainda):** deploy por SSH,
  monitoramento externo, observabilidade completa.
