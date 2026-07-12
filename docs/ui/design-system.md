# Design System — Contractor Platform

**Sprint:** 7A — Design Direction (refinamento)  
**Versão:** 1.1  
**Data:** 2026-07-12  
**Direcção Visual:** B — "Obra em Ordem"  
**Status:** Proposta sujeita a validação com logo real

---

> Todos os valores de cor são propostas baseadas na identidade descrita da marca JR Pinturas (primeiro tenant).  
> **Devem ser validados visualmente com a logo real antes de serem considerados definitivos.**  
> Este design system pertence ao Contractor Platform — os valores de cor são sobrescritos por tenant via CSS custom properties carregadas da API pública.

---

## Princípio: Tokens First

**Nenhum componente usa valores HEX directamente.** Todo o valor de cor, sombra, radius ou espaçamento é referenciado pelo token semântico (`var(--token-name)`).

Razões:
1. **Multi-tenant:** cada tenant sobrescreve os tokens de brand (`--primary`, `--background`, etc.) sem alterar os componentes
2. **Manutenção:** alterar uma cor num único lugar actualiza todos os usos
3. **Consistência:** impossível ter valores ligeiramente diferentes do mesmo conceito espalhados pelo código

**Proibido em componentes:**
```css
/* NUNCA */
color: #E8500A;
background: #1C1C1A;

/* SEMPRE */
color: var(--primary);
background: var(--surface-dark);
```

Os valores HEX neste documento existem apenas para documentar os tokens. O código CSS de componentes não os contém directamente.

---

## Tokens Semânticos de Cor

### Fundo e Superfície

```
--background:       #F8F6F2   /* Branco quente — fundo principal da página */
--foreground:       #1C1C1A   /* Carvão — texto principal sobre fundo claro */

--surface:          #FFFFFF   /* Branco puro — elevação, overlays, formulários */
--surface-muted:    #ECEAE5   /* Cinza-betão claro — secções alternadas, separadores de fundo */
--surface-dark:     #1C1C1A   /* Carvão — secções de destaque (barra de confiança, CTA final) */
--surface-dark-fg:  #F8F6F2   /* Texto sobre fundo escuro */
```

### Marca e Acento

```
--primary:           #E8500A   /* Laranja JR — acção principal, destaque */
--primary-hover:     #CC4508   /* Laranja escuro — hover de botão primário */
--primary-active:    #AA3B06   /* Laranja muito escuro — estado active/pressed */
--primary-foreground: #FFFFFF  /* Texto sobre laranja */

--accent:            #1C1C1A   /* Carvão como acento em contexto claro */
--accent-foreground: #F8F6F2   /* Texto sobre carvão */

--whatsapp:          #25D366   /* Verde WhatsApp — botão de contacto directo */
--whatsapp-hover:    #1EAD54   /* Verde WhatsApp escuro — hover */
--whatsapp-foreground: #FFFFFF /* Texto sobre verde WhatsApp */
```

### Sistema de Feedback

```
--success:           #2A6040   /* Verde musgo — confirmação, validação */
--success-light:     #E8F4ED   /* Verde suave — background de mensagem de sucesso */
--success-foreground: #FFFFFF

--error:             #C0392B   /* Vermelho escuro — erro, campo inválido */
--error-light:       #FDECEA   /* Rosa suave — background de mensagem de erro */
--error-foreground:  #FFFFFF

--warning:           #A0610A   /* Âmbar — aviso */
--warning-light:     #FEF3E2
```

### Bordas e Separadores

```
--border:            #D4D0C8   /* Cinza quente — bordas padrão */
--border-muted:      #E8E5DF   /* Cinza muito suave — separadores subtis */
--border-strong:     #1C1C1A   /* Carvão — bordas de ênfase */
```

### Estados Interactivos

```
--focus:             #E8500A   /* Laranja — anel de foco visível (4px, offset 2px) */
--focus-inset:       #FFFFFF   /* Branco — espaço entre elemento e anel */
--disabled:          #B8B5AF   /* Cinza médio — texto e bordas de elementos desactivados */
--disabled-bg:       #F0EDE8   /* Fundo de elementos desactivados */
```

### Tipografia Muted

```
--muted-foreground:  #6B6860   /* Cinza quente — texto secundário, labels, metadata */
--subtle-foreground: #9B9890   /* Cinza ainda mais suave — placeholders, datas */
```

---

## Verificação de Contraste (WCAG 2.2 AA)

| Par | Ratio | Normal | Large | Estado |
|---|---|---|---|---|
| `--foreground` sobre `--background` | 16.2:1 | AA | AA | Passa |
| `--primary-foreground` sobre `--primary` | 4.8:1 | AA | AA | Passa |
| `--surface-dark-fg` sobre `--surface-dark` | 16.2:1 | AA | AA | Passa |
| `--muted-foreground` sobre `--background` | 5.4:1 | AA | AA | Passa |
| `--muted-foreground` sobre `--surface` | 5.4:1 | AA | AA | Passa |
| `--subtle-foreground` sobre `--background` | 3.4:1 | Falha | AA | Usar só acima de 18px |
| `--primary` sobre `--surface-dark` | 3.1:1 | Falha | AA | Usar só em texto grande |
| `--foreground` sobre `--surface-muted` | 14.1:1 | AA | AA | Passa |

> Nota: os ratios são estimativas calculadas sobre os hexadecimais propostos. Verificar com ferramenta (e.g. WebAIM Contrast Checker) após definição final da paleta.

---

## Tipografia

### Sistema de Fontes

```
--font-display:  'Barlow', system-ui, sans-serif     /* Headlines de impacto (hero, secções) */
--font-body:     'DM Sans', system-ui, sans-serif     /* Corpo de texto, interface */
--font-mono:     'JetBrains Mono', monospace          /* Código, dados numéricos (admin) */
```

**Justificativa da escolha:**
- **Barlow** — firmeza condensed sem agressividade. Peso 700/800 para display. Disponível no Google Fonts. Alternativa: Sora, Plus Jakarta Sans.
- **DM Sans** — legibilidade óptima a tamanhos pequenos, peso variável. Neutro sem ser genérico. Alternativa: Inter.
- Ambas são variáveis, sem custo de licença, e carregam bem com `font-display: swap`.

### Escala Tipográfica (Type Scale)

Baseada em proporção 1.250 (Major Third) com ajuste para uso real.

| Token | Size | Weight | Line Height | Letter Spacing | Uso |
|---|---|---|---|---|---|
| `--text-display-2xl` | 4.5rem (72px) | 800 | 1.0 | -0.04em | Hero principal |
| `--text-display-xl` | 3.5rem (56px) | 700 | 1.05 | -0.03em | Títulos de hero secundário |
| `--text-display-lg` | 2.5rem (40px) | 700 | 1.1 | -0.02em | Títulos de secção |
| `--text-display-md` | 1.875rem (30px) | 600 | 1.2 | -0.01em | Sub-títulos |
| `--text-lg` | 1.25rem (20px) | 400 | 1.6 | 0 | Lead text, intro paragraphs |
| `--text-base` | 1rem (16px) | 400 | 1.7 | 0 | Body padrão |
| `--text-sm` | 0.875rem (14px) | 400 | 1.6 | 0.01em | Captions, labels |
| `--text-xs` | 0.75rem (12px) | 500 | 1.5 | 0.02em | Labels de badge, metadata |

**Labels de secção (uppercase):**
- Font: `--font-display` ou `--font-body`
- Size: `--text-xs` (12px)
- Weight: 600
- Letter-spacing: 0.12em
- Transformação: `text-transform: uppercase`
- Cor: `--muted-foreground` ou `--primary`

---

## Spacing (Escala de Espaçamento)

Base: 4px

| Token | Valor | Uso |
|---|---|---|
| `--space-1` | 4px | Margem interna mínima |
| `--space-2` | 8px | Gap entre ícone e texto |
| `--space-3` | 12px | Padding interno de inputs |
| `--space-4` | 16px | Padding interno de botões, cards |
| `--space-5` | 20px | Gap entre elementos de formulário |
| `--space-6` | 24px | Padding de secção interna compacta |
| `--space-8` | 32px | Gap entre blocos de conteúdo |
| `--space-10` | 40px | Padding de card |
| `--space-12` | 48px | Separação entre sub-secções |
| `--space-16` | 64px | Padding de secção mobile |
| `--space-20` | 80px | Padding de secção tablet |
| `--space-24` | 96px | Padding de secção desktop |
| `--space-32` | 128px | Padding de secção hero |
| `--space-40` | 160px | Espaçamento máximo entre secções grandes |

---

## Grid

```
--grid-cols-mobile:   1
--grid-cols-tablet:   2
--grid-cols-desktop:  12

--container-max:      1280px
--container-px-mobile:  20px
--container-px-tablet:  32px
--container-px-desktop: 64px

--grid-gap-mobile:   16px
--grid-gap-tablet:   24px
--grid-gap-desktop:  32px
```

**Zonas de conteúdo em desktop (12 colunas):**
- Texto principal: cols 1–6 (50%)
- Imagem/conteúdo secundário: cols 7–12 (50%)
- Conteúdo centrado largo: cols 2–11 (83%)
- Conteúdo centrado estreito: cols 3–10 (66%)

---

## Border Radius

Filosofia: poucos cantos arredondados. A maioria dos elementos é rectangular.

```
--radius-none:   0px      /* Padrão — botões, cards, imagens */
--radius-sm:     2px      /* Apenas inputs e badges */
--radius-md:     4px      /* Tooltips, dropdowns */
--radius-lg:     8px      /* Modal, drawer — caso existam */
--radius-full:   9999px   /* NUNCA usar em botões ou elementos de marketing */
```

**Regra:** o `--radius-full` é reservado exclusivamente para avatares circulares (se existirem no admin). Jamais em CTAs, cards de serviço ou qualquer elemento da landing.

---

## Bordas

```
--border-width-thin:    1px   /* Bordas padrão de input e card */
--border-width-medium:  2px   /* Hover state, focus visible */
--border-width-accent:  3px   /* Detalhe de marca (linha lateral laranja) */
--border-width-heavy:   4px   /* Focus ring acessível */
```

---

## Sombras

Filosofia: sombras mínimas. O espaço faz o trabalho de separação.

```
--shadow-none:   none

--shadow-xs:     0 1px 2px rgba(28, 28, 26, 0.06)
                 /* Input focus, elementos ligeiramente elevados */

--shadow-sm:     0 2px 4px rgba(28, 28, 26, 0.08)
                 /* Cards opcionalmente elevados */

--shadow-modal:  0 8px 32px rgba(28, 28, 26, 0.16)
                 /* Modais, drawers */
```

**Proibido:** `box-shadow` com blur acima de 32px, sombras coloridas, múltiplas sombras empilhadas para efeito "flutuante".

---

## Ícones

```
Sistema:  Phosphor Icons
Versão:   ^2.0.0
Pesos:    Regular (UI genérica), Bold (acções e destaques), Duotone (proibido na landing)
Tamanhos: 16px | 20px | 24px | 32px
```

| Contexto | Tamanho | Peso |
|---|---|---|
| Inline com texto de body | 16px | Regular |
| Lista de serviços | 20px | Bold |
| Acção de formulário | 20px | Regular |
| Botão com ícone | 20px | Bold |
| Ícone isolado de destaque | 32px | Bold |
| Navegação mobile | 24px | Regular |

---

## Botões

### Primário (CTA principal)

```
background:    var(--primary)           /* #E8500A */
color:         var(--primary-foreground) /* #FFFFFF */
border:        none
border-radius: var(--radius-none)       /* 0px */
padding:       14px 28px
font-size:     var(--text-base)         /* 16px */
font-weight:   600
letter-spacing: 0.01em
transition:    background 180ms ease

hover:    background: var(--primary-hover)
focus:    outline: 4px solid var(--focus), outline-offset: 2px
active:   background: var(--primary-active)
disabled: background: var(--disabled-bg), color: var(--disabled)
```

### Secundário (outline)

```
background:    transparent
color:         var(--foreground)
border:        2px solid var(--foreground)
border-radius: var(--radius-none)
padding:       12px 26px (compensa a borda)

hover: background: var(--foreground), color: var(--surface)
```

### Ghost

```
background:    transparent
color:         var(--foreground)
border:        none
padding:       12px 16px
underline:     no
hover:         background: var(--surface-muted)
```

### WhatsApp

```
background:    var(--whatsapp)
color:         var(--whatsapp-foreground)
border:        none
border-radius: var(--radius-none)
padding:       14px 28px
/* Ícone WhatsApp à esquerda */

hover:  background: var(--whatsapp-hover)
focus:  outline: 4px solid var(--focus), outline-offset: 2px
```

---

## Links

```
color:         var(--foreground)
text-decoration: underline (solid, 1px, offset 2px)
hover:         color: var(--primary)
visited:       color: var(--muted-foreground)
focus:         outline: 4px solid var(--focus), outline-offset: 2px
```

---

## Inputs

```
background:    var(--surface)
color:         var(--foreground)
border:        1px solid var(--border)
border-radius: var(--radius-sm)          /* 2px */
padding:       12px 16px
font-size:     var(--text-base)

placeholder-color: var(--subtle-foreground)

focus:  border: 2px solid var(--foreground), outline: 4px solid var(--focus), outline-offset: 2px
error:  border: 2px solid var(--error)
valid:  border: 1px solid var(--success)
disabled: background: var(--disabled-bg), color: var(--disabled), cursor: not-allowed
```

**Label:**
```
font-size:   var(--text-sm)
font-weight: 500
color:       var(--foreground)
margin-bottom: var(--space-2)
```

**Mensagem de erro:**
```
font-size: var(--text-sm)
color:     var(--error)
margin-top: var(--space-1)
```

---

## Cards

Filosofia: usar o mínimo possível. Preferir listas, grelhas e secções a cartões flutuantes.

Quando necessário:

```
background:    var(--surface)
border:        1px solid var(--border-muted)
border-radius: var(--radius-none)
padding:       var(--space-8) var(--space-8)    /* 32px */
box-shadow:    var(--shadow-none)               /* Sem sombra por defeito */

/* Variante elevada (admin apenas): */
box-shadow:    var(--shadow-sm)
```

**Proibido na landing:** `border-radius: rounded-xl`, `shadow-xl`, `backdrop-blur`.

---

## Estados de Foco (Acessibilidade)

Todos os elementos interactivos têm foco visível explícito:

```css
:focus-visible {
  outline: 4px solid var(--focus);
  outline-offset: 2px;
}

/* Em fundo escuro, inverter: */
.dark-bg :focus-visible {
  outline-color: var(--primary-foreground);
}
```

Nunca usar `outline: none` sem alternativa visível.

---

## Estados Desabilitados

```
opacity:        0.45
cursor:         not-allowed
pointer-events: none (quando aplicável)
/* Não remover o elemento do DOM — manter acessível para leitores de ecrã com aria-disabled */
```

---

## Animações

### Duração e Easing

```
--duration-instant:  0ms      /* Mudanças imediatas (toggle de visibilidade) */
--duration-fast:     120ms    /* Hover states */
--duration-base:     200ms    /* Transições de estado padrão */
--duration-slow:     320ms    /* Entrada de elementos */
--duration-enter:    480ms    /* Animação de secção ao entrar no viewport */
--duration-counter:  800ms    /* Count-up de estatísticas */

--ease-default:      ease
--ease-out:          cubic-bezier(0, 0, 0.2, 1)
--ease-in:           cubic-bezier(0.4, 0, 1, 1)
--ease-in-out:       cubic-bezier(0.4, 0, 0.2, 1)
--ease-spring:       cubic-bezier(0.34, 1.56, 0.64, 1)   /* Micro-interacções discretas */
```

### Reduced Motion

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

---

## Tokens de Estrutura de Página

```
--header-height-mobile:   56px
--header-height-desktop:  72px
--cta-fixed-height:       60px    /* CTA flutuante de WhatsApp em mobile */
--section-pt:             var(--space-16)  /* padding-top de secção */
--section-pb:             var(--space-16)  /* padding-bottom de secção */
```
