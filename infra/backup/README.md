# Backup / Restore — Contractor Platform (Sprint 11A.5)

Implementação de
[docs/design/DT-011A.5-backup-restore.md](../../docs/design/DT-011A.5-backup-restore.md).
Este README é o manual operacional — "como rodar", não "por que" (o DT tem a
justificativa completa de cada decisão).

**Nesta sprint:** tudo aqui foi validado com um **repositório Restic local**
(um diretório neste host). A Hetzner Storage Box real, credenciais reais e a
VPS real são Sprint 11B — nada disso foi comprado/configurado agora. Trocar de
repositório local para Storage Box real é, propositalmente, **só uma variável**
(`RESTIC_REPOSITORY`) — nenhum script muda.

---

## Arquitetura

```
scripts host-side (infra/backup/scripts/)
  │
  ├── backup-postgres.sh  ──► docker exec postgres pg_dump ──► staging efêmero ──► restic backup
  ├── backup-files.sh     ──► docker run --volumes-from backend:ro / caddy:ro ──► restic backup
  ├── backup-all.sh       ──► orquestra os dois acima + restic forget --prune
  ├── restore-postgres.sh ──► restic dump ──► validação ──► pg_restore (só com --confirm)
  └── restore-files.sh    ──► restic restore (staging) ──► validação ──► cópia controlada (só com --confirm)
                                                                              │
                                                                              ▼
                                                              Repositório Restic (criptografado)
                                                              local (esta sprint) / Storage Box (11B)
```

Nenhum serviço novo entra em `docker-compose.prod.yml`. Restic roda sempre como
container efêmero (`docker run --rm`, imagem `restic/restic`, versão pinada —
nunca `:latest`), nunca instalado dentro de `backend`/`frontend`/`postgres`/`caddy`.

Todos os scripts compartilham `infra/backup/scripts/lib.sh` (logging, validação
de pré-condições, resolução de containers via `docker compose ps -q`, e o
wrapper único `restic_run` que monta repositório/senha/cache/SFTP de forma
consistente) — não fazia parte da lista de 5 scripts do DT, mas evita repetir a
mesma lógica cinco vezes (ver "Diferenças em relação ao DT" no relatório da
sprint).

---

## Classificação dos dados

| Dado | Backup | Tag Restic |
|---|---|---|
| PostgreSQL (`postgres_data`, via `pg_dump`) | **Obrigatório** | `postgres` / `type=postgres` |
| `backend_storage` (uploads) | **Obrigatório** | `backend-storage` / `type=backend-storage` |
| `caddy_data` | **Recomendado/operacional** — não é dado crítico de negócio | `caddy-data` / `type=caddy-data` |
| `caddy_config` | **Não entra** — 100% derivado de `infra/caddy/Caddyfile`, já versionado | — |
| `production.env` | Entra **dentro** do repositório Restic junto do dump do Postgres (ver "Secrets" abaixo) | incluído na tag `postgres` |
| Código, migrations, imagens Docker, `node_modules`, `target/`, `.next/`, caches, logs | **Nunca** — reconstruíveis via git/GHCR/build | — |

---

## Setup (repositório local de teste)

```bash
cd infra/backup/env
cp backup.env.example backup.env
# editar backup.env — para teste local, os defaults já apontam para um
# repositório em disco (/var/backups/...); ajuste os caminhos se preferir
# outro lugar neste host

mkdir -p /var/backups/contractor-platform/restic-repo
mkdir -p /var/cache/contractor-platform/restic
mkdir -p /etc/contractor-platform   # ou outro diretório host-only equivalente

# Senha do repositório — NUNCA a mesma senha usada em outro lugar, e NUNCA
# commitada. Gerar uma vez:
openssl rand -base64 32 > /etc/contractor-platform/restic-password
chmod 600 /etc/contractor-platform/restic-password

# Inicializar o repositório (uma única vez por repositório):
docker run --rm \
  -e RESTIC_REPOSITORY=/var/backups/contractor-platform/restic-repo \
  -e RESTIC_PASSWORD_FILE=/run/secrets/restic-password \
  -v /etc/contractor-platform/restic-password:/run/secrets/restic-password:ro \
  -v /var/backups/contractor-platform/restic-repo:/var/backups/contractor-platform/restic-repo \
  restic/restic:0.19.1 init
```

A partir daqui, todos os comandos abaixo assumem que o stack de produção local
já está de pé (`infra/compose/docker-compose.prod.yml` + `infra/env/production.env`
— ver `infra/README.md`).

---

## Secrets

| Segredo | Onde vive | Entra no Restic? | Cópia externa obrigatória |
|---|---|---|---|
| `POSTGRES_PASSWORD` / `JWT_SECRET` (dentro de `production.env`) | `infra/env/production.env` (host, gitignored) | **Sim** — o próprio `production.env` é copiado para dentro do backup do Postgres, já criptografado pelo Restic (ver abaixo) | Não precisa — já está protegido pelo próprio mecanismo de backup |
| `RESTIC_PASSWORD` (conteúdo de `RESTIC_PASSWORD_FILE`) | Arquivo host-only, `chmod 600` | **Não pode** — é a chave que abre o próprio repositório (problema circular) | **Sim, obrigatório** — cópia num password manager externo a este host. Nenhum produto específico é escolhido nesta sprint. |
| Credenciais da Storage Box (usuário/chave SSH) | Arquivo host-only (`RESTIC_SFTP_KEY_FILE`), futuro (Sprint 11B) | **Não pode**, mesma razão acima | **Sim, obrigatório** — mesmo password manager externo |

**Nenhum destes segredos entra neste repositório Git, em nenhum momento.**
`infra/backup/env/backup.env` (o arquivo real, preenchido) é gitignored (mesma
regra `.env*` já aplicada a `infra/env/production.env`); só `backup.env.example`
é versionado, com placeholders.

### Por que `production.env` pode entrar no backup

Restic criptografa **todo** o conteúdo do repositório client-side, antes de
qualquer byte sair deste host — o que inclui o dump do Postgres, que agora
também contém `production.env` copiado para dentro do mesmo staging antes do
`restic backup` (ver `backup-postgres.sh` — o arquivo é lido apenas para
extrair `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD`; a cópia do arquivo em
si para dentro do repositório fica registrada como parte do mesmo snapshot
`postgres`, sob o path `/staging/production.env`, ao lado do `.dump`). Isso
resolve "como reconstituo a configuração da VPS depois de perdê-la inteira"
sem introduzir uma segunda ferramenta — é tão seguro quanto qualquer outro dado
dentro do mesmo repositório.

### Perda da `RESTIC_PASSWORD`

**É equivalente a perder o backup inteiro.** Sem essa senha, o repositório é
criptograficamente inacessível — não existe "recuperação de senha" do lado do
Restic. Por isso a cópia externa (password manager) não é opcional.

---

## Storage Box futura — SFTP

Protocolo escolhido: **SFTP** (único com suporte nativo no Restic; WebDAV
exigiria `rclone`, SMB não é suportado nativamente). Quando a Storage Box real
existir (Sprint 11B):

```bash
RESTIC_REPOSITORY=sftp:<storagebox-user>@<storagebox-host>:/contractor-platform
RESTIC_SFTP_KEY_FILE=/etc/contractor-platform/storagebox_ed25519   # chave privada, host-only
```

Nenhum hostname/usuário real é inventado aqui. `RESTIC_SFTP_COMMAND` (ver
`backup.env.example`) só é necessário se o comando `sftp` padrão que o Restic
deriva de `RESTIC_REPOSITORY` não bastar (ex.: caminho de chave não-padrão).
**Esta parte específica (SFTP contra a Storage Box real) não foi testada nesta
sprint** — só o backend `sftp`/`ssh` já confirmado presente na imagem
`restic/restic:0.19.1` foi verificado.

---

## Backup manual

```bash
cd infra/backup/scripts

# Tudo (Postgres + backend_storage + caddy_data + retenção):
./backup-all.sh

# Só o banco:
./backup-postgres.sh

# Só arquivos (ambos os volumes, ou um de cada vez):
./backup-files.sh                       # backend_storage + caddy_data
./backup-files.sh --target backend-storage
./backup-files.sh --target caddy-data

# Backup sem aplicar retenção ainda (útil ao testar):
./backup-all.sh --skip-retention

# Backup + uma checagem leve do repositório logo em seguida:
./backup-all.sh --with-check
```

## Snapshots

```bash
docker run --rm --env-file <(echo "RESTIC_REPOSITORY=$RESTIC_REPOSITORY") ... # ver restic_run em lib.sh
```

Na prática, use os próprios scripts (que já chamam isso internamente) ou, para
inspecionar manualmente, reaproveite exatamente a mesma invocação que
`lib.sh#restic_run` monta:

```bash
docker run --rm \
  -e RESTIC_REPOSITORY -e RESTIC_PASSWORD_FILE=/run/secrets/restic-password \
  -v "$RESTIC_PASSWORD_FILE":/run/secrets/restic-password:ro \
  -v "$RESTIC_CACHE_DIR":/root/.cache/restic \
  restic/restic:0.19.1 snapshots
```

## Retenção

```bash
# Já embutido em backup-all.sh, mas para rodar isoladamente/inspecionar antes:
... restic forget --keep-daily 7 --keep-weekly 4 --keep-monthly 6 --group-by tags --dry-run
```

Política: **7 diários + 4 semanais + 6 mensais**, aplicada **por tag**
(`--group-by tags`) — Postgres, `backend_storage` e `caddy_data` são podados
independentemente, nunca misturados no mesmo pool de retenção. Sempre valide
com `--dry-run` antes de rodar sem ele, especialmente ao testar pela primeira
vez ou depois de mudar os valores em `backup.env`.

## Verificação de integridade (`restic check`)

**Não precisa rodar `restic check` completo todos os dias** — o custo cresce
com o tamanho do repositório. Recomendado:

- **Leve** (sem `--read-data`), semanal: confirma a estrutura do repositório
  sem baixar todo o conteúdo — `./backup-all.sh --with-check` já inclui isso.
- **Completo** (`--read-data`), mensal: baixa e verifica cada blob de fato —
  rode manualmente: `... restic check --read-data`.
- Backup sem teste de restore não conta como backup — ver "Plano de testes"
  no relatório da sprint; repita esse teste periodicamente, não só uma vez.

---

## Restore de PostgreSQL

```bash
cd infra/backup/scripts

# 1) Dry run — resolve e valida o dump, NÃO toca no banco:
./restore-postgres.sh latest
# ou um snapshot específico:
./restore-postgres.sh <snapshot-id>

# 2) Só com confirmação explícita o restore de fato acontece:
./restore-postgres.sh latest --confirm
```

O que acontece com `--confirm`: para o container `backend` (evita escrita
durante o restore), roda `pg_restore --clean --if-exists --no-owner` dentro do
container `postgres` já em execução (via stdin, sem arquivo extra dentro do
container), e imprime os próximos passos — **não sobe o `backend` de volta
sozinho**. "Banco limpo" aqui significa `--clean --if-exists` (remove os
objetos conflitantes antes de recriá-los), não um `DROP DATABASE`/`CREATE
DATABASE` — ver comentário no topo de `restore-postgres.sh` para o motivo.

**Aviso explícito, sempre impresso ao final:** o dump restaurado pode refletir
um schema **anterior** ao que a imagem `backend` atualmente implantada espera.
O Flyway não faz downgrade. Se o backend falhar ao subir contra o banco
restaurado, escolha um `APP_VERSION` compatível
([DT-011A.4](../../docs/design/DT-011A.4-ghcr-image-pipeline.md)) antes de
tentar de novo — **nenhum downgrade automático de migration é implementado.**

## Restore de `backend_storage` / `caddy_data`

```bash
# Dry run — restaura para um diretório temporário, valida, NÃO toca no volume real:
./restore-files.sh --target backend-storage latest
./restore-files.sh --target caddy-data latest

# Com confirmação — copia o conteúdo validado para dentro do volume real:
./restore-files.sh --target backend-storage latest --confirm
```

Fluxo: snapshot → diretório temporário (`umask 077`, `chmod 700`) → validação
(arquivos de tamanho zero são sinalizados, nunca silenciosamente aceitos) →
cópia controlada via container utilitário (`--volumes-from <container>`,
gravável só nesta etapa) → `chown` para `backend_storage` (uid:gid `1000:1000`,
inspecionado em `backend/Dockerfile`, não hardcoded sem verificação — ver
`BACKUP_BACKEND_UID`/`BACKUP_BACKEND_GID` em `backup.env`). `caddy_data` não
precisa de `chown`: a imagem oficial `caddy:2-alpine` roda como root, mesmo
usuário do container utilitário que faz a cópia. Nenhum dos dois exige parar o
serviço correspondente — a cópia acontece com o container vivo.

### Arquivo individual

```bash
./restore-files.sh --target backend-storage latest \
  --path "company/<companyId>/logo/<uuid>.png" --confirm
```

`--path` é relativo à raiz do volume (`/app/storage` para `backend-storage`,
`/data` para `caddy-data`) — restaura só aquele arquivo, sem tocar no resto da
árvore.

---

## Disaster Recovery Total (VPS nova hipotética)

Não executado nesta sprint (não há VPS real) — roteiro para quando a VPS
existir (Sprint 11B):

1. Preparar o host: instalar Docker Engine + Docker Compose v2.
2. Obter o repositório: `git clone` deste repositório na VPS nova.
3. Configurar credenciais externas: chave SSH da Storage Box e
   `RESTIC_PASSWORD_FILE`, recuperados do password manager (nunca do próprio
   Restic — ver "Secrets" acima) — copiar/gerar os arquivos host-only que
   `backup.env` espera.
4. Recuperar `production.env` **de dentro do próprio Restic**:
   ```bash
   ./restore-postgres.sh latest   # sem --confirm — só para localizar o snapshot
   # o snapshot 'postgres' contém /staging/production.env ao lado do .dump:
   ... restic dump <snapshot> /staging/production.env --tag postgres > infra/env/production.env
   ```
5. `docker compose --env-file infra/env/production.env -f infra/compose/docker-compose.prod.yml pull` — usa o `APP_VERSION` já publicado no GHCR ([DT-011A.4](../../docs/design/DT-011A.4-ghcr-image-pipeline.md)), sem rebuild.
6. Subir `postgres` vazio (compose já cria o volume `postgres_data` do zero).
7. Restaurar PostgreSQL (`restore-postgres.sh ... --confirm`).
8. Subir `backend`/`frontend`, restaurar `backend_storage` (`restore-files.sh --target backend-storage ... --confirm`).
9. Opcionalmente restaurar `caddy_data` (`restore-files.sh --target caddy-data ... --confirm`) — ou deixar o Caddy reconstruir do zero a partir do `Caddyfile` versionado (aceitável, `caddy_data` é recomendado, não obrigatório).
10. Subir o Caddy, validar `docker compose ps` (todos `healthy`), roteamento `/`, `/api/*`, `/uploads/*` (mesmas validações da Sprint 11A.3), login e smoke test de dado.

**RTO estimado: algumas horas**, dependente de um operador humano seguindo
este roteiro — não há automação de restore total, não há alta disponibilidade.

---

## systemd (templates versionados, não instalados)

`infra/backup/systemd/contractor-platform-backup.{service,timer}` — **não
instalados nem habilitados** nesta sprint, nem neste Mac nem em qualquer VPS.
Quando a VPS real existir:

```bash
sudo cp infra/backup/systemd/contractor-platform-backup.* /etc/systemd/system/
# editar WorkingDirectory=/ExecStart= no .service para o caminho real do checkout
sudo systemctl daemon-reload
sudo systemctl enable --now contractor-platform-backup.timer
```

`OnCalendar=*-*-* 03:15:00 UTC` — default ajustável, **não é SLA**.
`Persistent=true` garante que um backup perdido por reboot/desligamento no
horário agendado roda assim que o sistema volta. O `.service` é `Type=oneshot`
e chama só `backup-all.sh` — nenhum segredo fica embutido no unit file (o
próprio script resolve `backup.env` sozinho).

---

## Observabilidade

Sem Prometheus/Grafana/Loki. Sinal de falha:

```bash
systemctl status contractor-platform-backup.service   # exit code da última execução
journalctl -u contractor-platform-backup.service       # log completo
journalctl -u contractor-platform-backup.timer         # próxima execução agendada
```

Notificação além disso (webhook/e-mail via `OnFailure=`) é extensão futura, não
implementada agora.

---

## RPO / RTO

- **RPO ≤ 24h** — decorre da frequência diária. **Não é SLA contratual**, é a
  premissa técnica inicial deste MVP.
- **RTO ≤ algumas horas**, operação manual (runbook acima) — sem alta
  disponibilidade, sem failover automático.

---

## Relação com GHCR / `APP_VERSION` / Flyway

- **Rollback de imagem (`APP_VERSION=<sha-anterior>`, DT-011A.4) reverte
  código, nunca dado.** Uma migration destrutiva que já rodou permanece
  aplicada mesmo com o container revertido — o código antigo pode inclusive
  quebrar contra um schema que não reconhece.
- **Restore (esta sprint) é o único mecanismo que reverte dado** — mais
  custoso (downtime, possível perda desde o último snapshot) que um rollback
  de imagem.
- **Nenhum downgrade automático de migration Flyway existe.** Uma migration
  destrutiva só é desfeita restaurando um snapshot anterior a ela.

---

## Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| `backup-postgres.sh` falha em "pg_dump failed" | Postgres indisponível, credencial errada, ou `pg_isready` já havia falhado antes | `docker compose ps postgres`; conferir `POSTGRES_*` em `production.env` |
| `backup-*.sh` falha com "required variable not set" | `backup.env` incompleto | Comparar com `backup.env.example` |
| `restic_run` falha com erro de repositório | `RESTIC_REPOSITORY` errado/inacessível, ou repositório ainda não inicializado (`restic init`) | Conferir o caminho; rodar `restic init` uma vez |
| `restic` falha com erro de senha | `RESTIC_PASSWORD_FILE` aponta para o arquivo errado, ou a senha não confere com a do `restic init` original | Nunca há "recuperar senha" — se a senha certa foi perdida, o repositório é inacessível (ver "Secrets") |
| `restic` reclama de lock | Uma execução anterior morreu no meio (queda de rede, kill) | `... restic unlock` — só depois de confirmar que não há outro backup rodando de verdade |
| `restore-postgres.sh`/`restore-files.sh` saem com código 2 | Comportamento esperado sem `--confirm` — é um dry run deliberado | Revisar a saída, rodar de novo com `--confirm` quando estiver certo |
| Restore de `backend_storage` com arquivos do dono errado | `BACKUP_BACKEND_UID`/`GID` não bate com o UID/GID real do container `backend` | Conferir `backend/Dockerfile` (`adduser -u ...`); ajustar `backup.env` |
| `docker compose ps -q <service>` retorna vazio | Serviço não está rodando | `docker compose -f infra/compose/docker-compose.prod.yml --env-file infra/env/production.env ps` |

---

## Riscos conhecidos (ver DT-011A.5 §32 para a lista completa)

- **Divergência temporal entre backup do banco e de `backend_storage`** — os
  dois rodam na mesma janela (`backup-all.sh`), mas não são transacionalmente
  atômicos entre si; um `companyId` criado entre os dois passos pode aparecer
  num backup e não no outro.
- **Upload capturado incompleto** — o backup de `backend_storage` é
  crash-consistent, não transacional; um upload em voo no instante exato do
  backup pode aparecer truncado num snapshot específico (risco estreito, ver
  DT-011A.5 §5.4/§9.2 — nomes de arquivo únicos por upload limitam o dano a
  esse snapshot).
- **Acesso ao socket Docker equivale a root no host** — limitação conhecida de
  qualquer automação baseada em Docker, não específica desta sprint.
