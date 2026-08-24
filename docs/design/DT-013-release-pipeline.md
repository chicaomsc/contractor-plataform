# DT-013 — Automated Release Pipeline

## Contexto

Até esta sprint, toda publicação de imagem era um evento técnico: `publish-images.yml` builda e
publica `backend`/`frontend`/`caddy` a cada push em `main`, tagueadas por SHA de commit
(`:<full-sha>`, `:<short-sha>`, `:main`). Isso é correto e continua existindo — é o caminho de
build contínuo/técnico, útil para validação e para o `platform-ops` operar no modelo legado
(`versions.env`, ver `infra/README.md` § `APP_VERSION`).

O que faltava: uma noção de **release** — uma versão SemVer única, humana, imutável, ligada a um
changelog legível, que o `platform-ops` já sabe consumir (schema `release.yml`,
`apiVersion: platform-ops/v1` — ver o próprio repositório `platform-ops`, ADR-006/007). Esta DT
define como esse conceito é produzido neste repositório.

## Decisão

Adotar [Release Please](https://github.com/googleapis/release-please) (Conventional Commits →
SemVer → Release PR → tag → GitHub Release → changelog), com a publicação de artefatos separada
em um segundo workflow, disparado pela release já criada — nunca pelo merge em si.

```
feature/fix branch → PR → CI → merge em main
                                    ↓
                         release-please.yml observa main
                                    ↓
                    mantém 1 Release PR (changelog + próxima versão)
                                    ↓
                         merge do Release PR (humano)
                                    ↓
              tag vX.Y.Z + GitHub Release criadas automaticamente
                                    ↓
                  release-publish.yml (gatilho: release published)
                                    ↓
        build (valida os 3) → publica (SemVer + sha-<sha>) → captura digest
                                    ↓
                    release-manifest.yml gerado e anexado
                                    ↓
        (Sprint 2C.1 — ainda não implementado: PR no platform-ops)
```

## Por que dois workflows, não um

`release-please.yml` decide **o quê** (versão, changelog, tag, GitHub Release).
`release-publish.yml` decide **como publicar o artefato** (build, push, digest, manifest). São
responsabilidades genuinamente diferentes, com gatilhos, permissões e falhas independentes — um
workflow que builda imagens de produção não deveria também ter `contents: write` para mexer em
release/changelog, e vice-versa. Separar também significa que uma falha de build não deixa o
Release PR/tag/GitHub Release num estado ambíguo — a release **já existe** e é válida
independentemente de a publicação de imagem ter sucesso (ver "Estratégia de falha" abaixo).

## Fluxo de desenvolvimento (formalizado, não tecnicamente bloqueado ainda)

```
feature/<algo>, fix/<algo>, chore/<algo>, ... → Pull Request para main → CI obrigatório → review → merge
```

Nenhum desenvolvimento normal depende de commit direto em `main`. **Branch protection não foi
configurada tecnicamente nesta sprint** (exigiria acesso de admin ao repositório no GitHub, fora
do escopo de uma sprint que só cria arquivos) — recomendação documentada em `CONTRIBUTING.md`
para configuração manual: exigir PR antes de merge, exigir os checks `Backend CI`/`Frontend CI`
como obrigatórios, proibir push direto em `main` (inclusive para admins, se a política da equipe
permitir).

## Conventional Commits → SemVer

| Prefixo | Bump |
|---|---|
| `fix:` | PATCH |
| `feat:` | MINOR |
| `feat!:`, `fix!:`, ou rodapé `BREAKING CHANGE:` | MAJOR |
| `chore:`, `docs:`, `test:`, `refactor:`, `ci:`, `build:`, `style:` | Nenhum bump por si só (a menos que marcado como breaking) |

Detalhamento e exemplos em `CONTRIBUTING.md`. O cálculo em si é feito pelo Release Please —
nenhuma lógica própria foi implementada aqui.

## Release Please — release única de produto (não por componente)

`release-please-config.json`: `"release-type": "simple"`, um único pacote em `"."`. Este é o
ponto central que impede o cenário indesejado `backend 1.0.0 / frontend 2.0.0 / caddy 3.0.0` —
existe **uma** versão para o produto inteiro (Vantry), independente de o repositório ser um
monorepo tecnicamente. `release-type: simple` não toca `frontend/package.json` nem
`backend/pom.xml` — só atualiza `.release-please-manifest.json` (rastreio interno) e
`CHANGELOG.md`. `frontend/package.json` mantém seu próprio campo `version` (hoje `0.7.0`),
inalterado por este pipeline — são dois conceitos de versão que não se misturam: um é metadado
interno do pacote npm (nunca lido pelo build/deploy), o outro é a identidade pública da release.

## Bootstrap da primeira release (`1.0.0`)

Confirmado (documentação oficial do Release Please): **sem uma entrada prévia no manifest para um
pacote, a primeira release calculada é sempre `1.0.0`**, independente do tipo dos commits
encontrados. `.release-please-manifest.json` foi criado como `{}` (nenhuma versão anterior
registrada) — não `{"." : "1.0.0"}`, que teria o efeito **oposto** do desejado (diria ao Release
Please "1.0.0 já foi lançada", fazendo-o calcular a *próxima* versão a partir daí).

`bootstrap-sha` foi fixado no commit `HEAD` no momento desta sprint
(`816b007e0737a706fa721a111df6e3f37a7eb852`) — instrui o Release Please a considerar, para o
changelog da primeira release, apenas commits a partir daqui, não os ~90 commits de todo o
histórico do projeto (Sprints 11A–12.4.2), que seriam ruído para um changelog de release pública.
Efeito prático: nenhum Release PR aparece imediatamente após esta sprint ser mergeada — só depois
que o primeiro commit `feat`/`fix` real for mergeado em `main` depois disso, o que é
intencionalmente consistente com "não criar release real ainda" desta sprint.

Nenhum `package-name`/`initial-version` adicional foi necessário — não existe como campo de
config direto no Release Please; o comportamento acima (manifest vazio → 1.0.0) já cobre
exatamente o requisito.

## Workflow de release — dois arquivos, dois gatilhos

### `release-please.yml`

- Gatilho: `push` em `main` (todo push é reavaliado; o Release PR é criado/atualizado
  conforme necessário, nunca uma tag é criada nesse momento).
- Permissions do job: `contents: write`, `pull-requests: write`, `actions: write` (só para o
  passo de dispatch — ver abaixo).
- `googleapis/release-please-action@v4`, sem `token:` customizado — `GITHUB_TOKEN` do job é
  suficiente para tudo que este workflow precisa fazer diretamente.

### `release-publish.yml`

- Gatilhos: `release: published` (idiomático) **e** `workflow_dispatch` com input `tag`
  (fallback — ver limitação abaixo).
- Permissions do workflow: piso `contents: read`; o job `publish` eleva para
  `contents: write, packages: write` (upload de asset + push de imagem); `check-immutability`
  usa `packages: read` (só inspeção).

### Limitação real do GitHub (documentada antes de qualquer workaround, por instrução explícita)

Eventos criados usando o `GITHUB_TOKEN` padrão de um job **não disparam outros workflows** —
proteção anti-recursão do próprio GitHub, não específica do Release Please. Isso significa que a
tag/GitHub Release criadas por `release-please.yml` (usando `GITHUB_TOKEN`) **não disparariam**
`release: published` em `release-publish.yml` sozinhas.

Duas soluções possíveis: (a) um Personal Access Token dedicado, passado ao Release Please, fazendo
a release "parecer" criada por um humano; (b) usar a exceção documentada do próprio GitHub —
`workflow_dispatch`/`repository_dispatch` **são** isentos dessa restrição quando o token tem
`actions: write`. Escolhida a opção (b): nenhum token adicional foi introduzido. Ao final de
`release-please.yml`, se `release_created == true`, um passo dispara explicitamente
`release-publish.yml` via `gh workflow run ... -f tag=<tag>`, usando só o `GITHUB_TOKEN` do
próprio job. `release-publish.yml` mantém `release: published` como gatilho igualmente válido
(funciona sozinho se, no futuro, uma release for criada por um humano ou por um PAT) — os dois
gatilhos coexistem sem conflito.

## Estratégia de tags OCI

Para a release `X.Y.Z`, cada imagem recebe duas tags publicadas no mesmo push (mesmo digest):

- `X.Y.Z` — identidade humana da release (SemVer). Imutável por política e por verificação ativa
  (ver "Imutabilidade" abaixo).
- `sha-<full-sha-do-commit>` — proveniência técnica, rastreável ao commit exato.

`:latest` nunca é publicada (`flavor: latest=false`, já era assim no `publish-images.yml`
existente — mantido). O caminho técnico (`:<sha>`, `:<short-sha>`, `:main`) de
`publish-images.yml` continua existindo, inalterado, para builds de desenvolvimento/CI contínuo —
os dois caminhos coexistem sem conflito (tags diferentes, nunca a mesma imagem tagueada com
`:main` e `:1.2.0` ao mesmo tempo pelo mesmo workflow).

## Imutabilidade

Antes de qualquer build, `check-immutability` roda `docker manifest inspect` para as três tags
`<image>:<versão>` — se qualquer uma já existir no GHCR, o workflow falha imediatamente
(`exit 1`), sem buildar nada. Uma release nunca é sobrescrita; uma correção é sempre a próxima
versão (PATCH/MINOR/MAJOR conforme o caso).

## Digests — capturados na origem, nunca inferidos depois

`docker/build-push-action@v6` (com `push: true`) expõe `steps.<id>.outputs.digest` — o digest
real que o GHCR atribuiu à imagem recém-publicada, retornado pelo próprio processo de push. Os
três digests (`backend`, `frontend`, `caddy`) são capturados assim, diretamente do job `publish`
— nunca descobertos depois inspecionando uma VPS ou qualquer outro processo indireto (a auditoria
do `platform-ops`, Sprint 2A, já mostrou que `RepoDigests` local pode simplesmente não existir —
essa fragilidade não se aplica aqui porque a fonte é o próprio ato de publicar, não uma inspeção
posterior).

## Release Manifest — gerado, anexado, não enviado ao platform-ops ainda

Após os três pushes, `release-manifest.yml` (schema `apiVersion: platform-ops/v1`, idêntico ao já
definido no `platform-ops`) é gerado com os valores reais de tag/digest/commit e:

1. Anexado como **artifact do próprio workflow run** (`actions/upload-artifact` — inspecionável a
   partir da execução, útil para depuração, sujeito à retenção padrão do Actions).
2. Anexado como **asset da GitHub Release** (`gh release upload`) — localização durável,
   descoberta natural por quem abre a release "Vantry X.Y.Z" no GitHub, sem precisar achar a
   execução do workflow que a gerou.

As duas, não uma — cobrem necessidades diferentes (debug imediato vs. referência permanente).
`environment: production` está fixo no manifest porque só existe um ambiente Vantry no
`platform-ops` hoje; se/quando existirem outros, isso precisa ser revisitado (não é mais uma
constante segura). **Nenhum PR é aberto no `platform-ops` nesta sprint** — o manifesto fica
disponível para a Sprint 2C.1 consumir.

## GitHub Release

Criada pelo Release Please (título "vX.Y.Z" + changelog, gerados automaticamente). Nenhum digest é
inserido manualmente no corpo da release — eles só existem depois do build, que roda em
`release-publish.yml`, minutos depois da release já ter sido criada; editar o corpo da release
depois seria um segundo ponto de escrita desnecessário. Por isso `release-manifest.yml` (que
carrega os digests) é anexado como **asset**, não inserido no texto.

## Permissions — mínimo por job, não por workflow inteiro

| Job | Permissions | Por quê |
|---|---|---|
| `release-please` (release-please.yml) | `contents: write`, `pull-requests: write`, `actions: write` | Release PR + tag/Release + dispatch do workflow seguinte |
| `resolve`, `build-*` (release-publish.yml) | herdadas do piso do workflow (`contents: read`) | Só checkout e build local, nunca push |
| `check-immutability` | `contents: read`, `packages: read` | Só inspeção de manifesto no GHCR |
| `publish` | `contents: write`, `packages: write` | Push de imagem + upload de asset na release |
| `ci-backend`/`ci-frontend` (reusable) | Herdadas do chamador, como já era em `publish-images.yml` | Nenhuma mudança de padrão |

## Estratégia de falha parcial

Cenário considerado: `backend` publicado, `frontend` publicado, `caddy` falha.

Docker/GHCR não oferece uma transação multi-imagem — não existe "desfazer" um push já concluído
com segurança (apagar um pacote do GHCR é uma ação manual, destrutiva, fora do escopo de
automação). A estratégia adotada minimiza — mas não elimina — o risco:

1. **Fase de build (sem push) roda para os três, em paralelo, antes de qualquer push.** A causa
   mais comum de falha (erro de compilação/build) nunca chega a publicar nada — se um Dockerfile
   não builda, `publish` nunca começa.
2. **Se ainda assim uma falha ocorrer durante a fase de push** (ex.: falha transitória de rede
   contra o GHCR), a release para essa versão é tratada como **contaminada — nunca reutilizada**.
   Nenhuma tentativa automática de limpeza/reuso. Um passo de aviso explícito (`if: failure()`)
   deixa isso registrado no log e no Job Summary, instruindo verificação manual de quais imagens
   foram de fato publicadas.
3. **Recuperação:** corrigir a causa raiz e publicar a **próxima** versão (ex.: um PATCH) —
   nunca tentar "completar" a versão que falhou parcialmente.

## Segurança

Nenhum PAT foi adicionado (ver seção de limitação acima). Nenhum script imprime `GITHUB_TOKEN`
nem qualquer secret. `release-manifest.yml` contém apenas SemVer/SHA/digest — nenhuma credencial.
`NEXT_PUBLIC_*` (GitHub Actions Variables, não secrets) continuam vindo de `vars.*`, exatamente
como já era em `publish-images.yml`.

## Concorrência

`release-please.yml`: `group: release-please` (nunca dois jobs mexendo no mesmo Release PR ao
mesmo tempo). `release-publish.yml`: `group: release-publish-<tag>` (escopado por tag/versão —
duas releases *diferentes* podem, em princípio, publicar em paralelo sem conflito; a mesma versão
nunca publica duas vezes simultaneamente). Nenhum dos dois cancela uma execução em andamento
(`cancel-in-progress: false`) — cancelar um push já em curso poderia deixar um manifesto
parcialmente enviado ao GHCR.

## O que não foi feito nesta sprint (deliberado)

Nenhuma release real foi criada, nenhuma tag real, nenhuma imagem `1.0.0` publicada. Nenhuma
alteração em `platform-ops`, VPS, Cloudflare, banco ou `docker-compose.prod.yml`. `APP_VERSION` e
`versions.env` (no `platform-ops`) permanecem exatamente como estavam. A automação de PR para o
`platform-ops` a partir do `release-manifest.yml` gerado aqui é a Sprint 2C.1.
