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
o cálculo automático de versão feito pelo Release Please — não escreva o CHANGELOG manualmente,
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
main → Release Please mantém um único "Release PR" (changelog + próxima versão calculada)
     → merge do Release PR (humano, quando decidir cortar uma release)
     → tag vX.Y.Z + GitHub Release criadas automaticamente
     → build das três imagens (backend/frontend/caddy), tag SemVer + digest capturado
     → release-manifest.yml gerado e anexado à release
```

### "Eu preciso criar tag manualmente?"

**Não.** `git tag` nunca é necessário no fluxo normal. A tag é criada automaticamente quando o
Release PR é mergeado.

### "Eu preciso lembrar qual é a última versão?"

**Não.** O Release Please calcula a próxima versão a partir dos tipos de commit desde a última
release — você nunca precisa saber ou decidir "é 1.2.0 ou 1.3.0".

### Merge de PR de feature/fix **vs.** merge do Release PR

São duas coisas diferentes:

- **Merge de um PR normal** (`feature/*`, `fix/*`, ...): adiciona código a `main`. Não cria
  release, não builda/publica nenhuma imagem SemVer (o pipeline técnico de `main`,
  `publish-images.yml`, continua publicando por SHA normalmente — isso é independente).
- **Merge do Release PR** (aberto e mantido automaticamente pelo Release Please, nunca por você):
  é o único gatilho que cria a tag, a GitHub Release, e dispara a publicação das imagens com tag
  SemVer. Só existe um Release PR aberto por vez, sempre atualizado pelo Release Please a cada
  push em `main`.

Você decide **quando** cortar uma release simplesmente mergeando o Release PR quando fizer
sentido — não a cada push, não automaticamente.

## Primeira release (`1.0.0`)

Não existe ainda. O Release Please a calculará automaticamente como `1.0.0` assim que houver pelo
menos um commit `feat`/`fix` elegível em `main` depois desta sprint — nenhuma configuração manual
de versão inicial é necessária.
