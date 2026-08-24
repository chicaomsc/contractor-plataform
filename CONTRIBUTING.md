# Contribuindo

Guia rápido do fluxo de desenvolvimento e de release deste repositório. Detalhamento técnico das
decisões: [docs/design/DT-013-release-pipeline.md](docs/design/DT-013-release-pipeline.md).

## Fluxo diário

```
branch (feature/fix/chore/...) → Pull Request para main → CI → review → merge
```

- Crie uma branch a partir de `main`: `feature/<algo>`, `fix/<algo>`, `chore/<algo>`,
  `docs/<algo>`, `refactor/<algo>`, `test/<algo>`, `ci/<algo>`, `build/<algo>`.
- Abra um Pull Request para `main`. `Backend CI`/`Frontend CI` rodam automaticamente conforme os
  arquivos alterados.
- Nenhum desenvolvimento normal depende de commit direto em `main`.

**Branch protection recomendada** (configuração manual no GitHub — Settings → Branches — não
aplicada tecnicamente por esta sprint, pois exige acesso de admin ao repositório):
- Exigir Pull Request antes de merge em `main`.
- Exigir os checks `Backend CI` e `Frontend CI` como obrigatórios antes de merge.
- Proibir push direto em `main` (avaliar se também para administradores).

## Conventional Commits

Toda mensagem de commit no formato `<tipo>[escopo opcional]: <descrição>`. O **tipo** determina
o cálculo automático de versão feito pelo semantic-release — não escreva o CHANGELOG manualmente,
não decida a versão manualmente.

| Tipo | Efeito na versão | Exemplo |
|---|---|---|
| `fix:` | PATCH (1.0.0 → 1.0.1) | `fix: corrige timeout no login de tenant` |
| `feat:` | MINOR (1.0.0 → 1.1.0) | `feat: adiciona exportação de relatório em PDF` |
| `feat!:` ou `fix!:` ou rodapé `BREAKING CHANGE: ...` | MAJOR (1.0.0 → 2.0.0) | `feat!: remove endpoint legado /api/v1/auth` |
| `chore:`, `docs:`, `test:`, `refactor:`, `ci:`, `build:`, `style:` | Nenhum bump por si só | `docs: atualiza README de infra` |

Um commit `chore`/`docs`/etc. só provoca bump se explicitamente marcado como breaking (`!` ou
rodapé `BREAKING CHANGE:`) — na prática, isso quase nunca acontece para esses tipos.

## Fluxo de release

```
main → semantic-release analisa os commits desde a última release
     → se houver ao menos um feat/fix/breaking: calcula a próxima versão,
       cria a tag e a GitHub Release automaticamente, no mesmo push
     → se não houver (só docs/chore/test/ci/build/refactor/style): nada acontece
       (RESULT=NO_RELEASE_REQUIRED) — não é uma falha
     → build das três imagens (backend/frontend/caddy), tag SemVer + digest capturado
     → release-manifest.yml gerado e anexado à release
     → PR automática em platform-ops (apps/vantry/production/release.yml)
     → aprovação humana da PR em platform-ops → só então produção pode ser atualizada
```

**Não existe mais Release PR.** Um merge normal para `main` já é, por si só, suficiente para
disparar uma release — não há um segundo PR intermediário para revisar/mergear dentro deste
repositório. Os únicos dois gates humanos do fluxo inteiro são: (1) a PR normal para `main`, (2)
a PR de promoção aberta automaticamente em `platform-ops`.

### "Eu preciso criar tag manualmente?"

**Não.** `git tag` nunca é necessário. A tag é criada automaticamente pelo semantic-release assim
que uma PR com commit `feat`/`fix`/breaking é mergeada em `main`.

### "Eu preciso criar uma Release PR?"

**Não.** Esse conceito não existe mais neste repositório (existia com Release Please, na Sprint
2C — removido na Sprint 2C.1). Mergear sua PR normal para `main` já é o gatilho.

### "Eu preciso escolher a versão?"

**Não.** semantic-release calcula a próxima versão a partir dos tipos de commit desde a última
release (tag `vantry-vX.Y.Z`) — você nunca decide "é 1.2.0 ou 1.3.0".

### "Meu merge em `main` vai direto para produção?"

**Não.** Merge em `main` cria uma release (tag + GitHub Release + imagens publicadas no GHCR) —
isso ainda não é produção. Produção só é atualizada quando alguém aprova e mergeia a PR aberta
automaticamente em `platform-ops` (`apps/vantry/production/release.yml`), e mesmo depois desse
merge o deploy em si continua sendo um `workflow_dispatch` manual de
`deploy-production.yml` naquele repositório — nunca automático.

### Todo merge em `main` cria uma release?

**Não.** Um merge de PR só cria release se houver pelo menos um commit `feat:`/`fix:`/breaking
entre os commits daquele PR. Um PR só com `docs:`/`chore:`/`test:`/`ci:`/`build:`/`refactor:`/
`style:` não cria nada — o workflow termina com sucesso (`RESULT=NO_RELEASE_REQUIRED`), não é um
erro.

## Primeira release (`1.0.0`)

A tag `vantry-v1.0.0` e a GitHub Release correspondente já existem (criadas em 2026-08-23) e são
usadas como base de cálculo pelo semantic-release. **Isso não significa que `1.0.0` esteja em
produção** — o pipeline anterior falhou antes de publicar as imagens, capturar digests, gerar o
manifest ou abrir a PR de promoção no `platform-ops`; produção continua no contrato legado
(`versions.env`). Detalhamento completo do estado e da classificação (`RELEASE_CREATED` /
`ARTIFACT_PUBLICATION_NOT_COMPLETED` / `NOT_PROMOTED`):
[DT-013 § Estado real da versão 1.0.0](docs/design/DT-013-release-pipeline.md#estado-real-da-versão-100--classificação-explícita).

A próxima release, seja ela `1.0.1` (fix), `1.1.0` (feat) ou `2.0.0` (breaking), é calculada
automaticamente a partir de `vantry-v1.0.0` — nenhuma configuração manual adicional — e, dessa vez,
com o pipeline corrigido, percorre o fluxo completo até a promoção.
