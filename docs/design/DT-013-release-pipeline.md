# DT-013 — Automated Release Pipeline

## Contexto

Até a Sprint 2C, toda publicação de imagem era um evento técnico: `publish-images.yml` builda e
publica `backend`/`frontend`/`caddy` a cada push em `main`, tagueadas por SHA de commit. Isso é
correto e continua existindo — é o caminho de build contínuo/técnico, útil para validação e para
o `platform-ops` operar no modelo legado (`versions.env`).

O que faltava: uma noção de **release** — uma versão SemVer única, humana, imutável, ligada a um
changelog legível, que o `platform-ops` já sabe consumir (schema `release.yml`,
`apiVersion: platform-ops/v1` — ver o próprio repositório `platform-ops`, ADR-006/007). A Sprint
2C implementou isso com Release Please. Depois de testar o fluxo contra o GitHub real, ficou claro
que o modelo de Release PR do Release Please não era o desenho certo — ver "Por que
semantic-release, não Release Please" abaixo. Esta revisão (Sprint 2C.1) substitui a ferramenta e
completa o pipeline com a promoção automática para o `platform-ops` (item deixado pendente na
2C).

## Decisão

Adotar [semantic-release](https://github.com/semantic-release/semantic-release) (Conventional
Commits → SemVer → tag → GitHub Release → changelog, **sem Release PR**), mantendo a publicação de
artefatos separada em um segundo workflow — e adicionar um terceiro passo, novo nesta sprint: a
promoção automática, via Pull Request, para `platform-ops`.

```
feature/fix branch → PR → CI → merge em main
                                    ↓
                    release.yml roda a cada push em main
                                    ↓
        semantic-release analisa os commits desde a última release
                                    ↓
              feat/fix/breaking elegível?  ──não──→ RESULT=NO_RELEASE_REQUIRED (fim, sucesso)
                                    │
                                   sim
                                    ↓
              tag vantry-vX.Y.Z + GitHub Release criadas automaticamente
                            (no MESMO push, sem PR intermediária)
                                    ↓
                  release-publish.yml (dispatch explícito, outputs version+sha)
                                    ↓
        build (valida os 3) → publica (SemVer + sha-<sha>) → captura digest
                                    ↓
                    release-manifest.yml gerado e anexado à release
                                    ↓
       PR automática em platform-ops (apps/vantry/production/release.yml)
                                    ↓
                    aprovação humana da PR em platform-ops
                                    ↓
     (deploy em si continua workflow_dispatch manual — inalterado, fora de escopo)
```

## Por que semantic-release, não Release Please

Release Please **exige estruturalmente** uma Release PR — não é uma opção configurável, é o
mecanismo central da ferramenta: o estado "release pendente" vive numa branch/PR dedicada
(`release-please--branches--main--components--<package>`) até um humano mergeá-la; só nesse merge
a tag e a GitHub Release são criadas. Isso é incompatível com o requisito desta sprint: **dois
gates humanos no total** (PR para `main`, PR para `platform-ops`), sem nenhum PR intermediário
dentro deste repositório. Forçar Release Please a "não ter Release PR" não é uma opção de
configuração real da ferramenta — seria um encaixe artificial.

semantic-release, por desenho, faz exatamente o oposto: a cada push no branch configurado
(`main`), ele analisa os commits desde a última tag reconhecida e, se houver um tipo elegível, cria
a tag e a GitHub Release **no mesmo run**, sem abrir nenhum PR. Isso é o comportamento padrão da
ferramenta, não uma configuração incomum — é o modelo mais usado de semantic-release na indústria.

Trade-off aceito conscientemente: sem o estado explícito de "há uma release pendente, ainda não
cortada" que uma Release PR oferece (visibilidade de "o que vai para a próxima versão" antes de
decidir cortá-la). Dado que o requisito explícito desta sprint é "meu merge para main já deve ser
suficiente para liberar a próxima release", esse trade-off é o esperado, não um efeito colateral
indesejado.

Confirmado empiricamente (não só por documentação) antes de implementar: `semantic-release
--dry-run` rodado localmente contra o histórico real deste repositório, com `tagFormat:
"vantry-v${version}"`, reconheceu corretamente a tag real `vantry-v1.0.0` como última release e
ignorou as tags históricas não relacionadas (`v0.1.0`–`v1.0.3`, de antes deste pipeline existir);
um commit `feat:` de teste (nunca publicado) calculou corretamente `1.1.0`.

## Sem manifest file — o histórico de tags é a fonte de verdade

Release Please dependia de `.release-please-manifest.json` para rastrear "qual foi a última
versão". semantic-release não precisa disso — ele lista as tags do próprio Git que casam com
`tagFormat` e usa a mais recente como baseline. Uma classe inteira de bug (manifest desatualizado/
divergente do histórico real de tags) deixa de existir. `release-please-config.json` e
`.release-please-manifest.json` foram removidos; `.releaserc.json` é a nova configuração.

## Formato da tag: `vantry-v<versão>`, não `v<versão>`

Decisão deliberada, não incidental: `tagFormat: "vantry-v${version}"` em `.releaserc.json`.
Motivos:

1. **Continuidade com a release real já existente.** A PR #58 (Release Please, Sprint 2C) foi
   mergeada em 2026-08-23, criando de fato a tag `vantry-v1.0.0` e a GitHub Release "vantry:
   v1.0.0" — objetos reais e públicos no GitHub. **Isso não significa que a versão 1.0.0 esteja
   implantada em produção** — ver classificação de estado logo abaixo ("Estado real da versão
   1.0.0"); "real" aqui descreve o objeto Git/GitHub, não o ambiente de produção do Vantry.
   `tagFormat` precisa casar com isso para semantic-release reconhecer `1.0.0` como a última
   release, em vez de recalcular do zero.
2. **Evitar colisão com o histórico não relacionado.** Este repositório já tinha tags `v0.1.0`
   até `v1.0.3` de muito antes deste pipeline (versionamento manual de sprints anteriores,
   apontando para commits completamente diferentes). Um `tagFormat` bare (`v${version}`) faria
   semantic-release confundir essas tags antigas com o histórico real de releases deste pipeline.
   Um prefixo próprio (`vantry-v`) as torna automaticamente invisíveis para o cálculo — nenhuma
   delas casa com o padrão.

## Conventional Commits → SemVer

| Prefixo | Bump |
|---|---|
| `fix:` | PATCH |
| `feat:` | MINOR |
| `feat!:`, `fix!:`, ou rodapé `BREAKING CHANGE:` | MAJOR |
| `chore:`, `docs:`, `test:`, `refactor:`, `ci:`, `build:`, `style:` | Nenhum bump por si só (a menos que marcado como breaking) |

Idêntico à Sprint 2C — só a ferramenta que calcula mudou. `releaseRules` explícitas em
`.releaserc.json` tornam essa tabela parte do arquivo de configuração, não implícita no preset
padrão do `@semantic-release/commit-analyzer`.

**Bug real encontrado e corrigido durante os testes desta revisão:** passar `releaseRules`
customizadas ao `@semantic-release/commit-analyzer` **substitui** o conjunto de regras padrão do
preset — não o complementa. A primeira versão de `.releaserc.json` listava regras por `type`
(`feat`/`fix`/...) mas não incluía uma regra para `breaking: true`; testado com um commit `feat!:`
real, o resultado foi `MINOR` em vez de `MAJOR` (o commit caiu na regra `{type: "feat"}` antes de
qualquer verificação de breaking change). Corrigido adicionando `{"breaking": true, "release":
"major"}` como a **primeira** regra da lista (regras são avaliadas em ordem, a primeira que casa
vence). Reconfirmado com dois casos reais: `feat!:` no cabeçalho e `BREAKING CHANGE:` no rodapé —
ambos agora calculam `2.0.0` corretamente a partir da baseline `1.0.0`.

## semantic-release — release única de produto (não por componente)

Um único `.releaserc.json` na raiz do repositório, `branches: ["main"]`. Nenhum `package.json`
raiz foi criado — semantic-release roda via `npx`/action sem exigir um pacote npm no repositório
(este não é um pacote npm, é um monorepo de aplicação). `frontend/package.json` mantém seu próprio
campo `version` (hoje `0.7.0`), inalterado por este pipeline — dois conceitos de versão que não se
misturam: um é metadado interno do pacote npm (nunca lido pelo build/deploy), o outro é a
identidade pública da release.

## Bootstrap — a primeira release já aconteceu

Diferente da Sprint 2C (quando `1.0.0` ainda não existia), esta sprint começa com `vantry-v1.0.0`
real e já publicada. Não há um "bootstrap" a desenhar — a continuidade é resolvida inteiramente
pelo `tagFormat` (seção acima). A próxima release, seja `1.0.1` (fix) ou `1.1.0` (feat), é
calculada normalmente a partir de `1.0.0` na primeira vez que `release.yml` rodar depois desta
sprint ser mergeada.

## Changelog — gerado e anexado, não commitado de volta em `main`

`@semantic-release/changelog` gera `CHANGELOG.md` (mesmas categorias visíveis/ocultas de sempre:
Features/Bug Fixes/Performance Improvements/Reverts visíveis; chore/docs/style/refactor/test/
build/ci ocultos) — mas o arquivo é gerado **no runner**, durante o job, e anexado como asset da
GitHub Release (mesma estratégia de dois locais já usada para `release-manifest.yml`), **não**
commitado de volta em `main` via push do bot.

Decisão deliberada: `@semantic-release/git` (o plugin que faria esse commit-and-push) foi
propositalmente omitido. Um push direto do bot a `main` colidiria com a branch protection
recomendada em `CONTRIBUTING.md` (exigir PR antes de merge) assim que ela for de fato configurada
— exigiria uma regra de exceção/bypass para o bot, mais uma peça de configuração manual e mais uma
superfície de risco (um caminho de escrita em `main` fora do fluxo normal de PR). O changelog
completo, categorizado, permanece sempre disponível — no corpo de cada GitHub Release e como
asset baixável — sem essa exceção. Se no futuro for preferível ter `CHANGELOG.md` como um arquivo
rastreado na árvore do repositório, reavaliar então, junto da configuração real de branch
protection (ver `CONTRIBUTING.md` "Branch protection recomendada").

Nota sobre o `CHANGELOG.md` já commitado hoje na raiz do repositório: foi gerado pelo Release
Please (Sprint 2C) no merge da PR #58, contém só a entrada `1.0.0`, e **não será mais atualizado
por este pipeline** — passa a ser um snapshot histórico congelado. O changelog de cada release a
partir de agora vive inteiramente na GitHub Release correspondente (corpo + asset), não neste
arquivo. Removê-lo da árvore ou mantê-lo como está é uma decisão do time, não tomada por esta
sprint (nenhum arquivo já commitado em `main` foi apagado).

## Workflow de release — dois arquivos, dois gatilhos

### `release.yml` (substitui `release-please.yml`)

- Gatilho: `push` em `main` — nunca em push de feature branch, nunca em PR aberta (semantic-release
  só analisa o branch configurado, e só nesse contexto de push).
- Roda `cycjimmy/semantic-release-action@v4` com `extra_plugins` para
  `conventional-changelog-conventionalcommits` (peer dependency do preset) e
  `@semantic-release/changelog`.
- Se `new_release_published == true`: dispara `release-publish.yml` explicitamente (ver bug real
  corrigido, abaixo). Se `false`: log `RESULT=NO_RELEASE_REQUIRED`, workflow termina com sucesso.

### `release-publish.yml`

- Gatilhos: `workflow_dispatch` (inputs `version`, `sha` — caminho real, usado por `release.yml`)
  **e** `release: published` (fallback semanticamente correto, caso uma release seja publicada por
  um humano ou por um token diferente do `GITHUB_TOKEN` padrão de `release.yml` no futuro).
- Jobs inalterados da Sprint 2C: `resolve` → `ci-backend`/`ci-frontend` (gate) →
  `check-immutability` → `build-backend`/`build-frontend`/`build-caddy` (validação, sem push) →
  `publish` (build real + push + digest + manifest).
- Job novo desta sprint: `promote-platform-ops` (ver seção própria abaixo).

## Bug real corrigido: `fatal: not a git repository`

A execução real da Sprint 2C (release-please.yml → passo "Dispatch release-publish.yml") falhou
com `fatal: not a git repository`. Causa raiz, confirmada lendo o workflow linha a linha e
comparando com o comportamento documentado do `gh` CLI: o passo rodava `gh workflow run
release-publish.yml --ref main -f tag=...` num job cujo único passo anterior
(`googleapis/release-please-action@v4`) nunca fazia checkout do repositório — sem checkout, o
diretório de trabalho do runner não é um repositório Git, e o `gh` CLI tenta autodetectar
`owner/repo` a partir do remote Git local quando `--repo` não é passado explicitamente. Falha
antes mesmo de chamar a API.

**Corrigido** (não isoladamente — como parte desta redesenho, já que o passo equivalente continua
existindo em `release.yml`): `--repo "${{ github.repository }}"` explícito, eliminando qualquer
dependência de git local. Nenhum checkout foi adicionado a esse job — o fix é não depender de
autodetecção, não adicionar um passo desnecessário.

Um segundo bug, latente, nunca chegou a disparar porque o primeiro sempre falhava antes: a lógica
de `release-publish.yml` fazia `VERSION="${TAG#v}"` — em uma tag real `vantry-v1.0.0`, isso produz
a string inválida `antry-v1.0.0` (só o "v" isolado é removido, não o prefixo `vantry-v` inteiro).
**Corrigido estruturalmente, não com um patch de string:** `release.yml` agora passa `version` e
`sha` como outputs explícitos do próprio semantic-release (`steps.semrel.outputs.new_release_
version` / `github.sha`) via `workflow_dispatch` inputs — `release-publish.yml` nunca mais precisa
fazer parsing de uma tag para extrair a versão no caminho real (o parsing só existe como fallback,
no caminho `release: published`, e agora usa o prefixo correto).

## Estratégia de tags OCI, Imutabilidade, Digests

Inalterado da Sprint 2C. Para a release `X.Y.Z`: `X.Y.Z` (identidade humana, imutável) +
`sha-<full-sha>` (proveniência técnica) publicadas no mesmo push. `:latest` nunca é publicada.
`check-immutability` (`docker manifest inspect`) falha o workflow antes de buildar se qualquer tag
já existir no GHCR — cobre também idempotência de rerun (item 15: reexecutar para uma versão já
publicada falha aqui, de forma clara, sem sobrescrever nada). Digests capturados via
`steps.<id>.outputs.digest` do `docker/build-push-action@v6`, nunca inferidos depois.

## Release Manifest — agora enviado ao platform-ops

`release-manifest.yml` (schema `apiVersion: platform-ops/v1`, idêntico ao definido em
`platform-ops`, **não alterado nesta sprint**) continua gerado e anexado à GitHub Release (artifact
+ asset, mesma estratégia dupla da Sprint 2C). A diferença desta sprint: o mesmo conteúdo (versão,
sourceCommit, createdAt, os três componentes com tag+digest) é também escrito diretamente em
`apps/vantry/production/release.yml` no repositório `platform-ops`, como parte da PR de promoção —
ver seção seguinte.

## Promoção automática para `platform-ops` (novo nesta sprint)

Job `promote-platform-ops` em `release-publish.yml`, depois de `publish` ter sucesso:

1. Checkout de `platform-ops` usando `secrets.PLATFORM_OPS_TOKEN` (ver "Autenticação cross-repo"
   abaixo).
2. **Idempotência:** verifica se a branch `promote/vantry-<versão>` já existe em `platform-ops`
   (`git ls-remote --heads`) — se sim, não cria PR duplicada, apenas registra um aviso e encerra
   com sucesso.
3. Lê `apps/vantry/production/release.yml` **antes** de sobrescrevê-lo, para capturar a versão
   anterior (usada no corpo da PR). Na primeira promoção (caso real de hoje: o arquivo ainda não
   existe em `platform-ops`), a versão anterior é reportada como "nenhuma — primeira promoção".
4. Escreve `apps/vantry/production/release.yml` com os valores reais desta release (schema
   inalterado).
5. Cria a branch, commita, push, abre a PR via `gh pr create --repo chicaomsc/platform-ops`.

Título da PR: `chore(vantry): promote <versão> to production`. Corpo: tabela com versão anterior,
nova versão, source commit (link), link da GitHub Release, tabela de digests por componente, e uma
nota explícita de que mergear a PR **não** dispara deploy automaticamente — `deploy-production.yml`
continua exigindo `workflow_dispatch` manual, inalterado.

**Esta PR é o gate de produção.** Nenhum outro caminho deste pipeline chega a produção sem
aprovação humana explícita dela.

## Autenticação cross-repo — `PLATFORM_OPS_TOKEN`

`GITHUB_TOKEN` de uma Actions run em `contractor-plataform` não tem permissão sobre
`platform-ops` — isso é uma restrição de segurança do GitHub, não uma limitação a contornar com
gambiarra. Duas opções avaliadas: GitHub App (instalação própria, chave privada gerenciada,
melhor para múltiplas automações cross-repo) vs. Personal Access Token **fine-grained**, restrito a
um único repositório e a permissões nomeadas.

**Escolhido: PAT fine-grained**, nome do secret **`PLATFORM_OPS_TOKEN`**, guardado em
`contractor-plataform` (não em `platform-ops` — é o repositório que *usa* o token para escrever no
outro). Escopo mínimo necessário: acesso restrito ao repositório `platform-ops` apenas, permissões
`Contents: Read and write` + `Pull requests: Read and write`, nada além disso. Justificativa: uma
única automação, escrevendo em um único repositório, com duas permissões — a complexidade
operacional de um GitHub App (App próprio, instalação, rotação de chave privada) não se paga para
este caso de uso hoje; reavaliar se o número de automações cross-repo crescer.

**Este token não foi assumido como já existente** — não faz parte desta sprint criá-lo ou
imprimi-lo. `release-publish.yml` referencia `secrets.PLATFORM_OPS_TOKEN` e falha cedo, com uma
mensagem de erro clara apontando para o procedimento de setup, se o secret não estiver configurado.
Procedimento manual completo (passo a passo de criação do PAT, escopos exatos, onde salvar):
`platform-ops/docs/runbooks/setup-platform-ops-token.md`.

## GitHub Release

Criada por semantic-release (título/tag `vantry-vX.Y.Z`, corpo = changelog gerado pelo
`@semantic-release/release-notes-generator`). `release-manifest.yml` (que carrega os digests) só
existe depois do build, minutos depois — por isso é anexado como **asset** pela mesma lógica já
documentada na Sprint 2C, não inserido retroativamente no corpo da release.

## Permissions — mínimo por job

| Job | Permissions | Por quê |
|---|---|---|
| `release` (release.yml) | `contents: write`, `actions: write` | Tag + GitHub Release; dispatch do workflow seguinte |
| `resolve`, `build-*` (release-publish.yml) | herdadas do piso do workflow (`contents: read`) | Só checkout e build local, nunca push |
| `check-immutability` | `contents: read`, `packages: read` | Só inspeção de manifesto no GHCR |
| `publish` | `contents: write`, `packages: write` | Push de imagem + upload de asset na release |
| `promote-platform-ops` | `contents: read` (do `GITHUB_TOKEN` do job); toda escrita usa `secrets.PLATFORM_OPS_TOKEN`, nunca o `GITHUB_TOKEN` do job | Least privilege: o `GITHUB_TOKEN` deste repositório nunca precisa (nem consegue) escrever em `platform-ops` |
| `ci-backend`/`ci-frontend` (reusable) | Herdadas do chamador | Nenhuma mudança de padrão |

Do lado do `platform-ops`: `PLATFORM_OPS_TOKEN` restrito a esse único repositório, só `Contents` +
`Pull requests` (ver seção de autenticação acima) — sem acesso a nenhum outro repositório.

## Estratégia de falha parcial

Inalterada da Sprint 2C: fase de build (sem push) para os três, em paralelo, antes de qualquer
push real — a causa mais comum de falha (erro de build) nunca chega a publicar nada. Se uma falha
ainda assim ocorrer durante o push, a versão é tratada como **contaminada — nunca reutilizada**,
com aviso explícito (`if: failure()`) no log/summary; recuperação é sempre publicar a próxima
versão, nunca tentar completar a que falhou. `promote-platform-ops` só roda depois de `publish` ter
sucesso completo — uma publicação parcial nunca chega a abrir PR de promoção.

## Concorrência

`release.yml`: `group: release` (nunca duas execuções calculando a partir de um estado de tags
potencialmente inconsistente entre si). `release-publish.yml`: `group: release-publish-<versão>`
(versões diferentes publicam em paralelo sem conflito; a mesma versão nunca publica duas vezes).
`promote-platform-ops`: `group: promote-platform-ops-vantry-production`, deliberadamente **sem** a
versão no nome do grupo — impede duas promoções simultâneas do mesmo app/ambiente no
`platform-ops`, mesmo vindas de releases diferentes em voo ao mesmo tempo (item 17). Nenhum dos
grupos cancela uma execução em andamento (`cancel-in-progress: false`).

## Segurança

Nenhum script imprime `GITHUB_TOKEN`, `PLATFORM_OPS_TOKEN`, ou qualquer secret.
`release-manifest.yml`/`release.yml` (em `platform-ops`) contêm apenas SemVer/SHA/digest — nenhuma
credencial. `PLATFORM_OPS_TOKEN` nunca é passado para nenhum outro job/step além dos que
efetivamente precisam escrever em `platform-ops`.

## Migração do estado real (Sprint 2C.1)

Auditado antes de qualquer alteração (via API pública do GitHub, `git ls-remote`, `git log` — o
repositório é público, não foi necessário nenhum token para auditar):

- **PR #58** (`chore(main): release vantry 1.0.0`, aberta por `github-actions[bot]` via Release
  Please) foi mergeada em `main` em 2026-08-23 21:43 -03 (commit `507673b`). **Não foi mexida nem
  fechada por esta sprint** — já estava mergeada quando a auditoria começou.
- Isso criou de fato a tag `vantry-v1.0.0` (anotada, assinada pelo GitHub) e a GitHub Release
  "vantry: v1.0.0", publicada, `assets: []`. **Nenhuma das duas foi apagada ou recriada.**
- Tags `v0.1.0`–`v1.0.3`: histórico não relacionado, anterior a este pipeline, apontando para
  commits diferentes. **Não tocadas.**

### Estado real da versão 1.0.0 — classificação explícita

**1.0.0 NÃO está em produção.** O pipeline anterior (release-please.yml, Sprint 2C) falhou no
passo de dispatch (`fatal: not a git repository` — ver "Bug real corrigido" acima) antes de
`release-publish.yml` chegar a rodar. Como consequência, a cadeia parou logo depois do primeiro
dos três estágios:

| Estágio | Status | Evidência |
|---|---|---|
| **RELEASE_CREATED** | ✅ Concluído | Tag `vantry-v1.0.0` e GitHub Release "vantry: v1.0.0" existem, reais, publicadas no GitHub |
| **ARTIFACT_PUBLICATION_NOT_COMPLETED** | ❌ Não concluído | Nenhuma imagem `ghcr.io/chicaomsc/contractor-platform-{backend,frontend,caddy}:1.0.0` foi publicada; nenhum digest OCI foi capturado; nenhum `release-manifest.yml` foi gerado; GitHub Release "vantry: v1.0.0" tem `assets: []` |
| **NOT_PROMOTED** | ❌ Não concluído | `apps/vantry/production/release.yml` não existe em `platform-ops`; nenhuma PR de promoção foi aberta; produção continua executando o contrato legado (`versions.env`, SHA `adbfe3d3451ed372bd55308bbe977dec2d83ed35`) |

Estado limpo, apesar de incompleto: **não há publicação parcial nem artefato contaminado** — a
cadeia simplesmente nunca avançou além do primeiro estágio, então não há nada para reverter ou
limpar. `1.0.0` continua sendo uma release Git/GitHub válida e reconhecida como baseline pelo novo
pipeline (`tagFormat`) — apenas nunca foi publicada como imagem nem promovida.

**Consequência prática:** a versão `1.0.0` **nunca deve ser reutilizada** para fechar essa lacuna
manualmente (ex.: forçar um build/publish avulso apontando pra ela) — isso violaria a regra de
imutabilidade de release já documentada (`check-immutability`) no primeiro `push` real de
`ghcr.io/.../*:1.0.0`, mas de qualquer forma não é o caminho correto: a run real de
`release-publish.yml` que faltou (bloqueada pelo bug, agora corrigido) nunca aconteceu, então
recriá-la fora do fluxo automatizado reintroduziria exatamente o tipo de escrita manual que este
pipeline existe para eliminar. **O caminho correto é seguir em frente:** o próximo commit
`fix`/`feat`/breaking elegível mergeado em `main`, com o pipeline corrigido, publica `1.0.1`/
`1.1.0`/`2.0.0` normalmente — com imagens, digests, manifest e promoção completos — usando
`vantry-v1.0.0` como baseline de cálculo (não como algo a "completar" retroativamente).

### Limpeza de artefatos do pipeline anterior

- `.github/workflows/release-please.yml`, `release-please-config.json`,
  `.release-please-manifest.json`: removidos. `googleapis/release-please-action` nunca mais roda
  neste repositório — a branch `release-please--branches--main--components--vantry` (já mergeada
  via PR #58) fica órfã no GitHub; pode ser apagada manualmente quando conveniente, não é
  bloqueante (Release Please não a recriará, já que o workflow que a mantinha foi removido).

Nenhum `git commit`/`push`/`tag`/`release` foi executado por esta sprint sem autorização explícita
— todas as alterações descritas aqui existem localmente até serem revisadas.

## O que não foi feito nesta sprint (deliberado)

Nenhuma release real foi criada por esta sprint (a única real, `vantry-v1.0.0`, já existia antes,
criada pelo pipeline anterior). Nenhuma imagem publicada por esta sprint. Nenhuma alteração em
VPS, Cloudflare, banco, `docker-compose.prod.yml`, ou no schema `release.yml` do `platform-ops`.
Branch protection em `main` continua apenas recomendada/documentada (`CONTRIBUTING.md`), não
configurada tecnicamente — exigiria acesso de admin ao repositório, fora do escopo de uma sprint
que só cria/edita arquivos. `secrets.PLATFORM_OPS_TOKEN` não foi criado por esta sprint — apenas
referenciado, com o procedimento de criação documentado
(`platform-ops/docs/runbooks/setup-platform-ops-token.md`).
