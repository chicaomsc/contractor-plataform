# SPIKE-001 — Impeccable no comparador antes/depois

## Contexto

O Contractor Platform já possui Next.js App Router, integração pública por slug, DTO → Mapper → ViewModel, tokens semânticos, branding seguro e documentação visual para a direcção "Obra em Ordem".

A próxima sprint real será a landing pública. Antes de aplicar uma ferramenta assistida por IA em código definitivo, este spike avalia um único componente isolado: `BeforeAfterComparison`.

## Hipótese

O Impeccable poderia melhorar acabamento visual e clareza de interacção do comparador antes/depois sem descaracterizar a direcção aprovada, sem criar UI genérica e sem aumentar a complexidade de manutenção.

## Escopo

- Rota isolada: `/_lab/before-after`.
- Duas implementações com a mesma API de props.
- Fixture local, sem imagens externas instáveis.
- Slider desktop, imagens empilhadas em mobile, conforme documentação aprovada.
- Nenhuma secção definitiva da landing.
- Nenhum backend, endpoint ou regra de negócio.

## Implementação baseline

`BeforeAfterComparisonBaseline` usa uma composição manual directa:

- slider desktop por Pointer Events;
- teclado com `ArrowLeft`, `ArrowRight`, `Home` e `End`;
- `role="slider"` com `aria-valuemin`, `aria-valuemax`, `aria-valuenow` e `aria-valuetext`;
- labels "Antes" e "Depois";
- fallback acessível para par incompleto;
- mobile empilhado e tablet sem slider interactivo.

## Implementação com Impeccable

`BeforeAfterComparisonRefinedCandidate` foi criada como candidata refinada isolada, mas a ferramenta Impeccable não esteve disponível no ambiente:

- não existe dependência `impeccable` no projeto;
- não existe CLI `impeccable` no `PATH`;
- não existe plugin ou connector instalável compatível neste ambiente;
- a busca por ferramenta via `tool_search` não disponibilizou comando executável.

Portanto, esta versão não pode ser considerada uma saída real do Impeccable. Ela representa apenas uma implementação alternativa refinada manualmente para manter a comparação técnica lado a lado.

## Comandos e instruções utilizados

Comandos de descoberta:

```bash
rg -n "impeccable|Impeccable" .. .
command -v impeccable
```

Ferramentas de descoberta:

- `tool_search` para procurar ferramenta Impeccable disponível;
- `list_available_plugins_to_install` para verificar plugin/connector instalável.

Instrução que seria fornecida ao Impeccable se disponível:

```text
Refinar somente BeforeAfterComparisonRefinedCandidate, preservando a mesma API de props da baseline, tokens semânticos, direcção visual "Obra em Ordem", slider apenas em desktop, mobile empilhado, WCAG 2.2 AA, sem HEX, sem cards decorativos, sem gradientes SaaS e sem conteúdo específico de tenant.
```

## Alterações aceitas

Como a ferramenta não estava disponível, nenhuma alteração real do Impeccable foi aceite.

Na versão refinada manual foram aceites:

- uso de `figcaption` no modo empilhado;
- handle com ícone `MoveHorizontal` da biblioteca já existente;
- labels de imagem com fundo escuro para contraste em fotografia;
- destaque editorial por borda esquerda usando `--primary`;
- fallback textual mais específico para par incompleto.

## Alterações rejeitadas

Foram rejeitadas por antecipação, com base nos documentos do projeto:

- slider em mobile;
- glassmorphism;
- gradientes decorativos;
- cards desnecessários;
- sombras fortes;
- imagens externas;
- biblioteca de slider;
- animações elaboradas;
- conteúdo específico de tenant.

## Comparação

| Critério | Baseline | Versão refinada | Observação |
|---|---:|---:|---|
| Aderência à direção visual | 4 | 4 | Ambas respeitam tokens, fotografia e pouco arredondamento. |
| Hierarquia | 3 | 4 | A borda esquerda melhora a leitura da refinada. |
| Clareza | 4 | 4 | Ambas comunicam antes/depois sem excesso. |
| Responsividade | 5 | 5 | Ambas seguem mobile empilhado e desktop slider. |
| Acessibilidade | 5 | 5 | Mesma cobertura de teclado e ARIA. |
| Qualidade semântica | 4 | 5 | A refinada usa `figcaption` de forma mais forte. |
| Complexidade do código | 4 | 4 | Complexidade semelhante. |
| Quantidade de abstrações | 5 | 5 | Sem abstrações prematuras. |
| Manutenibilidade | 4 | 4 | Ambas são locais e previsíveis. |
| Performance | 5 | 5 | Sem dependência extra, `next/image`, aspect-ratio estável. |
| Aparência genérica de IA | 4 | 4 | Sem padrões SaaS, mas ainda usa fixture laboratorial. |
| Personalização multi-tenant | 5 | 5 | Tokens semânticos e dados por props. |
| Compatibilidade com tokens | 5 | 5 | Sem HEX em componentes. |
| Correções manuais necessárias | 5 | 1 | Impeccable não executou; não há correções mensuráveis. |
| Tempo de implementação | 4 | 1 | Não mensurável para Impeccable indisponível. |

## Acessibilidade

Coberto por testes:

- renderização de imagens e labels;
- `role="slider"`;
- `aria-valuenow` e `aria-valuetext`;
- `ArrowLeft`;
- `ArrowRight`;
- `Home`;
- `End`;
- `Tab`;
- `Shift+Tab`;
- fallback acessível quando falta imagem;
- ausência de conteúdo específico da JR Pinturas.

O foco visível usa o padrão global do projeto. O slider não depende apenas de drag.

## Responsividade

A decisão documentada foi preservada:

- mobile: imagens empilhadas;
- tablet: sem slider interactivo;
- desktop: slider com mouse, toque e teclado.

Slider mobile não foi adotado. Qualquer alteração futura precisa de nova decisão documentada.

## Performance

- `next/image` usado nas duas versões.
- `width`/`height` ou `fill` com `aspect-ratio` preservam espaço e reduzem CLS.
- Sem `priority`, pois a rota é laboratório e o componente futuro não é necessariamente LCP.
- Sem dependência de slider.
- Sem listeners globais.
- Eventos de pointer ficam no palco do componente.
- Custo de hidratação limitado a um Client Component interativo.

## Complexidade

A lógica comum ficou restrita a:

- tipos;
- fixture;
- `clampComparisonPosition`;
- `getComparisonValueText`;
- validação de par completo.

As duas versões não compartilham implementação visual.

## Limitações

- Impeccable não pôde ser executado neste ambiente.
- A versão refinada não prova qualidade real da ferramenta.
- A comparação visual usa fixtures locais, não fotografia real de obra.
- Não foram executados Lighthouse ou axe DevTools no navegador.

## Resultado

A baseline já atende ao comportamento e à acessibilidade necessários com baixa complexidade.

A versão refinada mostra pequenas melhorias semânticas e visuais, mas essas melhorias foram manuais e não atribuíveis ao Impeccable.

## Decisão

REJECT

O Impeccable não deve ser adotado para a Sprint 7C neste momento porque não houve execução reproduzível da ferramenta, não há comandos auditáveis, não há sugestões reais avaliadas e não é possível afirmar ganho de qualidade.

## Restrições de uso futuro

Se o Impeccable for reavaliado no futuro:

- usar apenas em componentes isolados;
- nunca aceitar output automaticamente;
- fornecer `docs/ui/visual-direction.md`, `design-system.md`, `component-principles.md`, `accessibility.md` e `responsive-behavior.md`;
- exigir testes de teclado, acessibilidade, lint, typecheck e build;
- proibir HEX em componentes;
- proibir alteração de contratos, hooks ou regras de negócio;
- registrar sugestões aceitas e rejeitadas.

## Impacto na Sprint 7C

A Sprint 7C deve seguir com implementação manual orientada pelo design system.

O componente de laboratório pode servir como referência técnica para o futuro `BeforeAfterComparison`, mas não deve ser importado como secção definitiva da landing sem revisão e adaptação.
