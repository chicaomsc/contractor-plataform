# Security Baseline — Automated Scans

**Sprint:** 11B.7 — Automated Security Scans
**Escopo:** automação de detecção contínua de vulnerabilidades (dependências, código,
segredos). Não altera regras de negócio, autenticação, endpoints ou contratos
públicos — ver `docs/design/DT-011B.5-security-hardening-plan.md` para o hardening
funcional já implementado nas Sprints 11B.6A–D.

## 1. Visão geral dos scanners

| Scanner | O que cobre | Onde roda | Bloqueia o build? |
|---|---|---|---|
| OWASP Dependency-Check | CVEs conhecidos em dependências Java (`backend/pom.xml`) | `./mvnw -Psecurity-scan verify` (local/CI), `security.yml` (agendado) | Não — `failBuildOnCVSS` acima do máximo possível |
| `npm audit` | CVEs conhecidos em dependências Node (`frontend/package.json`) | `npm run security:audit` (local/CI), `security.yml` (agendado) | Não — o comando pode sair com código ≠ 0, mas não faz parte do build normal |
| CodeQL | Padrões de código inseguro (SQL injection, XSS, path traversal, etc.) — Java e TypeScript/JavaScript | `security.yml` (agendado + push em `main`) | Não bloqueia PRs — resultados aparecem na aba *Security* do repositório |
| gitleaks | Segredos commitados (chaves, tokens, senhas, connection strings) | `security.yml` (agendado + push em `main`), `gitleaks detect` local | Não (`continue-on-error: true`) — mas é o scanner mais importante de revisar rapidamente quando aciona |
| Dependabot | Atualizações de dependências desatualizadas (não é um scanner de vulnerabilidade em si, mas mantém a superfície pequena) | Automático, PRs semanais | N/A — abre PR, não bloqueia nada |

Nenhum destes scanners faz parte do `./mvnw test`/`npm run build`/CI normal
(`backend-ci.yml`/`frontend-ci.yml`) — todos são aditivos, em `security.yml` ou
invocados sob demanda, exatamente como pedido nesta sprint ("não falhar o build
local por padrão").

## 2. Periodicidade

- **Agendado:** toda segunda-feira, 06:00 UTC (`security.yml`, `schedule: cron`) —
  antes do início da semana de trabalho.
- **Sob demanda:** `workflow_dispatch` no GitHub Actions, a qualquer momento.
- **Em todo push para `main`:** captura novas dependências assim que chegam à branch
  principal, sem esperar a próxima segunda-feira.
- **Local:** sempre que o desenvolvedor quiser, via os comandos da §3 — nunca
  automático em `git commit`/`git push`.

## 3. Comandos

### Backend

```bash
# Vulnerabilidades conhecidas (CVE) nas dependências — requer NVD_API_KEY (ver §3.1)
cd backend
./mvnw -Psecurity-scan verify -DskipTests -DnvdApiKey=<sua-chave>
# Relatórios em backend/target/dependency-check-report/ (HTML, XML, JSON)

# Dependências desatualizadas (não é CVE — é "existe uma versão mais nova?")
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates
```

#### 3.1 Pré-requisito: NVD_API_KEY

O `dependency-check-maven` 13.x **exige** uma chave de API do NVD para baixar os
dados de CVE — confirmado nesta sprint: sem chave, o plugin falha rápido com
`Invalid API Key`, tanto se `-DnvdApiKey` for omitido quanto se for passado vazio.
Não existe mais um modo anônimo funcional nesta versão.

1. Registrar gratuitamente em <https://nvd.nist.gov/developers/request-an-api-key>.
2. Uso local: `-DnvdApiKey=<chave>` no comando acima, ou `NVD_API_KEY` no ambiente.
3. Uso em CI: configurar o secret `NVD_API_KEY` no repositório GitHub (Settings →
   Secrets and variables → Actions) — sem isso, o job `dependency-check` de
   `security.yml` continua rodando (não quebra o workflow, `continue-on-error: true`)
   mas nunca produz um relatório real.

### Frontend

```bash
cd frontend

# Vulnerabilidades conhecidas (CVE) nas dependências
npm run security:audit
# equivalente a `npm audit` — não altera o build normal (`npm run build`)

# Dependências desatualizadas
npm run deps:outdated
# equivalente a `npm outdated`
```

### Secret scanning (local)

```bash
# Requer o binário gitleaks instalado (não é dependência do projeto)
gitleaks detect --config .gitleaks.toml --source .
```

## 4. Severidade e política de correção

| Severidade (CVSS / advisory) | Prazo alvo de correção | Ação |
|---|---|---|
| Critical | Imediato — antes do próximo deploy | Corrigir ou aplicar mitigação; se não houver correção disponível, registrar risco aceito explícito com responsável nomeado (mesmo padrão de `DT-011B.5 §10`) |
| High | Até 7 dias | Corrigir na próxima janela de manutenção técnica |
| Moderate/Medium | Até 30 dias | Agrupar com outras atualizações de rotina |
| Low | Sem prazo fixo | Backlog — corrigir quando conveniente (ex.: junto de outra mudança na mesma dependência) |

Vulnerabilidades **transitivas** (a dependência vulnerável não é direta, vem de
outra biblioteca) seguem a mesma tabela, mas a correção normalmente é "atualizar a
dependência direta que a traz", não um patch isolado — documentar a cadeia
(`X depende de Y depende de Z vulnerável`) na issue/PR de correção.

Nenhuma correção de dependência foi aplicada nesta sprint — o escopo era só
automação de detecção. O resultado do primeiro `npm audit` real (§ ver relatório da
entrega) já identificou itens a triar numa sprint dedicada de atualização de
dependências.

## 5. Como atualizar dependências

1. Rodar os comandos da §3 (`versions:display-dependency-updates` /
   `npm run deps:outdated`) para ver o que está desatualizado.
2. Priorizar pela tabela de severidade (§4) quando a atualização for motivada por
   uma vulnerabilidade — senão, por conveniência/rotina.
3. Atualizações **patch/minor**: normalmente seguras, podem ser feitas
   diretamente, rodando a suíte completa depois (`./mvnw test`;
   `npm run lint && npm run typecheck && npm run test && npm run build && npm run test:e2e:smoke`).
4. Atualizações **major**: exigem revisão do changelog da dependência antes —
   nunca só trocar o número da versão. `docs/security/dependency-audit.md` (Sprint 9)
   é um exemplo do formato esperado para registrar essa revisão.
5. **Dependabot** abre PRs automaticamente (semanal, limite de 5 simultâneos por
   ecossistema — Maven/npm/GitHub Actions, `.github/dependabot.yml`) — revisar como
   qualquer outro PR, rodando a suíte completa antes de aprovar.

## 6. Secret scanning — o que é coberto

`gitleaks` roda com seu conjunto de regras padrão (chaves AWS, tokens GitHub
`ghp_`/`gho_`/etc., chaves privadas, segredos genéricos de alta entropia) **mais**
regras específicas deste projeto (`.gitleaks.toml`):

- `JWT_SECRET` atribuído a um valor real (excluindo os dois placeholders conhecidos
  de `.env`/`.env.example`).
- Connection strings Postgres/JDBC com credenciais embutidas
  (`postgres://user:pass@host`).
- `POSTGRES_PASSWORD`/`DB_PASSWORD` atribuído a um valor real (mesma exclusão de
  placeholder).

**`.gitignore` verificado nesta sprint** — já cobre o essencial antes mesmo desta
automação existir:
- raiz: `.env`, `.env.*`
- `infra/env/.gitignore`: `*.env` com exceção explícita `!*.env.example` (cobre
  `production.env`, que não segue o padrão `.env.production` do `.gitignore` raiz)
- confirmado: `infra/env/production.env` (arquivo real, com segredos) nunca foi
  rastreado pelo git; só `production.env.example` (placeholders) está commitado.

**Se o gitleaks (ou o secret scanning nativo do GitHub) acionar um alarme:** tratar
a chave/token como comprometido imediatamente — revogar/rotacionar na origem
(AWS IAM, GitHub → Settings → Developer settings, `JWT_SECRET` no ambiente de
produção via `docs/operations/runbook.md`), **antes** de remover do histórico do
git (remover do histórico sozinho não invalida uma chave já vazada).

**Recomendação adicional (fora do escopo desta automação, requer acesso de
administrador do repositório):** habilitar o *secret scanning* e *push protection*
nativos do GitHub (Settings → Code security and analysis) — bloqueiam o próprio
`git push` de um segredo reconhecido, antes mesmo de chegar ao repositório. O
gitleaks em `security.yml` é um scanner adicional (roda no código já commitado,
cobre também regras customizadas do projeto), não um substituto.

## 7. CodeQL

Duas linguagens configuradas (`security.yml`, job `codeql`, matriz):
- `java-kotlin` (backend) — `build-mode: autobuild`.
- `javascript-typescript` (frontend) — `build-mode: none` (não precisa compilar
  para análise estática de JS/TS).

Resultados aparecem na aba **Security → Code scanning alerts** do repositório
GitHub — não em artefato de download (diferente de dependency-check/npm
audit/gitleaks, que também publicam artefato via `actions/upload-artifact`).

## 8. Arquivos relevantes

| Arquivo | Papel |
|---|---|
| `backend/pom.xml` (`security-scan` profile) | OWASP Dependency-Check |
| `backend/pom.xml` (`versions-maven-plugin`) | Checagem de dependências desatualizadas |
| `frontend/package.json` (`security:audit`, `deps:outdated`) | `npm audit`/`npm outdated` |
| `.github/workflows/security.yml` | Orquestra os 4 scanners em CI, agendado + manual + push em `main` |
| `.github/dependabot.yml` | Atualizações automáticas semanais |
| `.gitleaks.toml` | Regras customizadas de secret scanning |
| `docs/security/dependency-audit.md` | Snapshot manual de auditoria (Sprint 9) — histórico, não a política corrente |
| `docs/operations/runbook.md §21.4` | Operação: como rodar/triar localmente |
