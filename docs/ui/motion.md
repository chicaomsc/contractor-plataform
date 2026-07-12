# Motion Guide — Contractor Platform

**Sprint:** 7A — Design Direction (refinamento)  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Aplicação:** Landing page e futuro painel admin

---

## Filosofia de Animação

**Movimento existe para servir o conteúdo. Nunca para decorar.**

A JR Pinturas não é um produto de software. É uma empresa de ofício. O movimento deve reflectir isso: preciso, discreto, sem exibicionismo.

### Três perguntas antes de qualquer animação

1. **Remove fricção ou introduz distracção?** Se não remove fricção, não existe.
2. **O utilizador percebe que algo mudou sem a animação?** Se sim, a animação é decorativa.
3. **Resiste ao `prefers-reduced-motion`?** Se não resiste, não é acessível.

### O que as animações comunicam aqui

- **Confirmação de acção** — o utilizador fez algo, o sistema respondeu
- **Orientação de atenção** — guiar o olho para o novo conteúdo
- **Continuidade** — transições que mantêm o contexto espacial

### O que as animações não fazem aqui

- Não entretêm
- Não demonstram capacidade técnica
- Não tentam impressionar
- Não acontecem em todos os elementos ao mesmo tempo
- Não existem em "parallax" agressivo
- Não flutuam, não pulsam, não giram sem motivo

---

## Duração Padrão

```
--duration-instant:   0ms      /* Sem transição — toggle de visibilidade abrupta */
--duration-fast:      120ms    /* Hover de estado — a mais rápida perceptível */
--duration-base:      200ms    /* Transição padrão — estado de foco, cor de botão */
--duration-slow:      320ms    /* Entrada de elemento — fade + translate */
--duration-enter:     480ms    /* Secção a entrar no viewport */
--duration-counter:   800ms    /* Count-up de estatísticas */
--duration-drawer:    260ms    /* Abertura de menu lateral */
```

**Regra:** se a animação parece demorar, é demasiado longa. O utilizador não deve esperar.

---

## Easing

```
--ease-default:    ease
--ease-out:        cubic-bezier(0, 0, 0.2, 1)       /* Entrada — começa rápido, termina suave */
--ease-in:         cubic-bezier(0.4, 0, 1, 1)        /* Saída — começa suave, termina rápido */
--ease-in-out:     cubic-bezier(0.4, 0, 0.2, 1)      /* Transições bidirecionais */
--ease-spring:     cubic-bezier(0.34, 1.56, 0.64, 1) /* Micro-interacções discretas (handle do slider) */
```

**Nota sobre `--ease-spring`:** usar com cautela e apenas em interacções físicas (arrastar, toggle de switch). Nunca em entradas de página.

---

## Hover

Hover deve ser imediato na percepção e não deveria ter duração superior a 200ms.

| Elemento | Propriedade animada | Duração | Easing | Notas |
|---|---|---|---|---|
| Botão primário | `background-color` | 120ms | ease | Escurece 8% |
| Botão secundário | `background-color`, `color` | 120ms | ease | Inverte cores (fundo ↔ texto) |
| Link de navegação | `color` | 120ms | ease | Passa de `--foreground` para `--primary` |
| Link de texto | `text-decoration-color` | 120ms | ease | Underline aparece em laranja |
| Item de serviço | `border-left-color` | 150ms | ease-out | Linha laranja 3px desliza da esquerda |
| Imagem de galeria | `opacity` | 180ms | ease | Overlay subtil de 10% escuro |
| Card (se existir) | `box-shadow` | 180ms | ease-out | Sombra aparece (`--shadow-sm`) |

**Proibido em hover:**
- Scale (crescer/encolher elementos inteiros)
- Transform em Y (flutuar para cima)
- Múltiplas propriedades com diferentes durações

---

## Focus

O foco nunca deve ser animado — deve aparecer instantaneamente.

```
:focus-visible {
  outline: 4px solid var(--focus);
  outline-offset: 2px;
  /* Sem transition — foco imediato é obrigatório para acessibilidade */
}
```

**Razão:** utilizadores de teclado dependem do indicador de foco para navegar. Qualquer atraso é um obstáculo.

---

## Loading

### Botão em estado de carregamento

Após submissão de formulário:

```
Estado visual:
- Texto substituído por spinner (SVG circular, 20px)
- Background mantém-se (não esbate)
- disabled=true (bloqueia clique duplo)
- aria-busy="true"

Spinner: rotação contínua 360° em 600ms, linear, infinita
```

### Loading de página / dados da API

Não usar spinner de página inteira. Em vez disso:

**Skeleton Loading:**
- Blocos de placeholder com animação de pulso
- Cor: `--surface-muted` com brilho que percorre horizontalmente
- Animação: `shimmer` — gradiente que passa da esquerda para a direita

```
Shimmer:
- background: linear-gradient(90deg, --surface-muted 25%, --border-muted 50%, --surface-muted 75%)
- background-size: 200% 100%
- animation: shimmer 1.5s infinite linear
```

**Skeleton shapes por componente:**

| Componente | Skeleton |
|---|---|
| HeroSection | Rectângulo 100% × 240px (mobile), metade da página (desktop) |
| TrustStrip | 4 rectângulos 80px × 40px em linha |
| ServiceItem | Linha de 60% + linha de 40% + linha de 80% |
| GalleryItem | Quadrado proporção 4:3 |
| BeforeAfterComparison | Dois rectângulos 4:3 lado a lado |

---

## Transições de Estado (State Transitions)

### Mudança de conteúdo (galeria, tabs)

- **Saída:** `opacity: 1 → 0` em `--duration-fast` (120ms), `--ease-in`
- **Entrada:** `opacity: 0 → 1` em `--duration-base` (200ms), `--ease-out`
- Sem movimento em Y — apenas opacidade
- Conteúdo entra imediatamente após a saída (sem delay desnecessário)

### Toggle de visibilidade (FAQs, expandable)

- **Expandir:** `height: 0 → auto` com `overflow: hidden`; usar `grid-template-rows: 0fr → 1fr` para transição suave
- Duração: `--duration-slow` (320ms), `--ease-out`
- **Colapsar:** `--duration-base` (200ms), `--ease-in`

---

## Scroll Reveal

Elementos que entram no viewport têm uma entrada subtil. **Não usar em todos os elementos** — apenas em elementos de secção que beneficiam de orientação de atenção.

### Onde usar

- HeroSection (apenas o texto — a imagem não)
- TrustStrip (métricas individuais, escalonadas)
- Primeiros 2–3 items de serviços
- Processo de trabalho (cada passo com delay escalonado)

### Onde NÃO usar

- Header (fixo, já está presente)
- Footer
- Imagens de galeria (quebra a experiência de scroll rápido)
- CTA final (o utilizador já está comprometido)
- Qualquer elemento que já esteja visível sem scroll

### Especificação

```
Entrada padrão:
  from: opacity: 0; transform: translateY(12px)
  to:   opacity: 1; transform: translateY(0)
  duration: --duration-enter (480ms)
  easing: --ease-out
  trigger: IntersectionObserver (threshold: 0.1)

Delay escalonado (processo, métricas):
  item 1: delay 0ms
  item 2: delay 80ms
  item 3: delay 160ms
  item 4: delay 240ms
  (máximo 4 items escalonados)

Comportamento com reduced-motion:
  Conteúdo aparece imediatamente sem animação.
```

---

## Comparador Antes/Depois

O slider é o único componente da landing com interacção contínua. Deve seguir o ponteiro/toque sem latência.

### Movimento do handle

```
Posição do handle:
  - Segue o cursor/toque em tempo real
  - Sem CSS transition na posição X (acompanha imediatamente)
  - clip-path ou width da imagem actualiza frame a frame

Handle visual (círculo com setas):
  - Hover: scale(1.1) em 120ms ease — ligeiro crescimento
  - Drag activo: scale(0.95) — retracção táctil
  - Ambas as animações em transform (GPU)
```

### Comportamento de teclado

```
Arrow Left / Arrow Right:
  - Move o handle em 5% do total
  - Sem animação CSS — actualização imediata
  - Feedback: aria-valuenow actualizado

Home / End:
  - Move para 0% / 100%
  - Sem animação
```

---

## Abertura de Menus (Mobile)

### NavDrawer

```
Abertura:
  - translateX: 100% → 0
  - duration: --duration-drawer (260ms)
  - easing: --ease-out

Fecho:
  - translateX: 0 → 100%
  - duration: 200ms
  - easing: --ease-in

Backdrop (overlay escuro):
  - opacity: 0 → 0.5 (durante abertura do drawer)
  - opacity: 0.5 → 0 (durante fecho)
  - duration: equal ao drawer
```

### Ícone hamburguer → X

```
Linha superior: rotate(45deg) e translateY para centro
Linha do meio: opacity: 1 → 0
Linha inferior: rotate(-45deg) e translateY para centro

duration: --duration-base (200ms)
easing: --ease-in-out
```

---

## Comportamento Mobile

### Diferenças face ao desktop

| Comportamento | Desktop | Mobile |
|---|---|---|
| Hover states | Activos | Não existem (usar `:active` em vez de `:hover`) |
| Scroll reveal | Com translate Y | Apenas opacity (sem translate — evita jank) |
| Comparador slider | Drag com mouse | Não existe slider (imagens empilhadas) |
| Animação de entrada das secções | Sim | Sim (mas com `will-change: opacity` para GPU) |
| Skeleton loading | Sim | Sim |

### Performance mobile

```
Regras de CSS para animações performáticas:
  - Usar apenas: opacity, transform (translate, scale, rotate)
  - Evitar: width, height, margin, padding, top, left (causam reflow)
  - Adicionar: will-change: transform ao elemento antes da animação (remover depois)
  - Usar: backface-visibility: hidden em elementos com transform 3D
```

---

## Sumário: O que animar e o que não animar

| Elemento | Animar? | Nota |
|---|---|---|
| Hover de botão | Sim | Background, 120ms |
| Hover de link nav | Sim | Cor, 120ms |
| Foco visível | Não | Instantâneo — obrigatório |
| Entrada de secção (scroll) | Sim, com moderação | Só onde acrescenta orientação |
| Skeleton loading | Sim | Shimmer, 1.5s loop |
| Spinner de botão | Sim | Rotação linear, 600ms |
| Drawer mobile | Sim | translateX, 260ms |
| Galeria — troca de item | Sim | Fade, 200ms |
| Comparador — drag | Não (CSS) | Segue o cursor em tempo real |
| Handle do slider — hover | Sim | Scale 1.1, 120ms |
| Scroll parallax | Não | — |
| Elementos de decoração | Não | — |
| Texto a aparecer letra a letra | Não | — |
| Contador de estatísticas | Sim, opcional | Count-up, 800ms |
| Page transitions | Não | Desnecessário nesta fase |
