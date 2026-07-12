# Comportamento Responsivo — Landing Page JR Pinturas

**Sprint:** 7A — Design Direction  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Estratégia:** Mobile-first

---

## Estratégia Mobile-First

Todo o CSS é escrito primeiro para mobile (375px+). Breakpoints adicionam regras progressivamente.

**Princípio:** não degradar o desktop — enriquecer o mobile. A experiência mobile não é uma versão simplificada; é a versão principal.

**Razão:** o tráfego de pesquisa local em Portugal em dispositivos móveis supera 65% (estimativa conservadora para o perfil de público da JR Pinturas — pessoas a procurar serviços de pintura/remodelação no telemóvel durante o horário de trabalho ou em casa).

---

## Breakpoints

| Nome | Largura mínima | Descrição |
|---|---|---|
| `xs` (default) | 320px | Telemóvel pequeno — base do CSS |
| `sm` | 375px | iPhone standard — referência de design mobile |
| `md` | 640px | Landscape de telemóvel / telemóvel grande |
| `lg` | 768px | Tablet portrait |
| `xl` | 1024px | Tablet landscape / laptop pequeno |
| `2xl` | 1280px | Desktop padrão |
| `3xl` | 1536px | Desktop grande (max-width do container) |

**Container máximo:** `1280px` com `padding-inline: 20px (mobile) → 32px (tablet) → 64px (desktop)`.

---

## Grid por Breakpoint

| Breakpoint | Colunas | Gap | Comportamento |
|---|---|---|---|
| `< 640px` | 1 | 16px | Coluna única, full-width |
| `640px–767px` | 2 | 20px | Apenas onde faz sentido (serviços, stats) |
| `768px–1023px` | 2 | 24px | Layout 2 colunas para hero e sobre |
| `1024px–1279px` | 12 | 28px | Grid de 12 colunas, uso explícito |
| `1280px+` | 12 | 32px | Grid de 12 colunas, container max |

---

## Header — Comportamento Responsivo

### Mobile (< 768px)

```
┌─────────────────────────┐
│ [Logo]   [Nav]   [📱]   │
└─────────────────────────┘
```

- Altura: 56px fixo
- Logo à esquerda (max-width: 120px)
- Ícone de hamburguer ao centro-direito
- Ícone de WhatsApp no extremo direito (sempre visível, nunca escondido)
- Menu de navegação: drawer lateral (slide from right) ou dropdown completo
  - Backdrop escuro ao abrir
  - Fechado por swipe para direita, clique no backdrop ou ESC
  - Links de navegação em tamanho `--text-lg` (20px) para área de toque adequada

### Desktop (≥ 768px)

```
┌──────────────────────────────────────────────────┐
│ [Logo]     [Serviços] [Galeria] [Como funciona]  [📱 WhatsApp] │
└──────────────────────────────────────────────────┘
```

- Altura: 72px, reduz para 56px no scroll (transição `height 200ms ease`)
- Logo à esquerda
- Links de navegação ao centro (flexbox, `gap: 32px`)
- Botão WhatsApp à direita (botão completo com texto)

### Comportamento no scroll

- **Scroll para baixo > 80px:** header "comprimido" — logo reduz 10%, altura reduz, shadow `--shadow-xs` aparece
- **Scroll para cima:** header restaura
- Em mobile: não esconder o header ao scroll — a WhatsApp CTA fixa substitui essa função

---

## CTA Fixo Mobile (WhatsApp)

```
position: fixed
bottom: 20px
right: 20px
width: 56px
height: 56px
border-radius: 50% (excepção à regra — este é um botão circular de acção rápida)
background: #25D366 (WhatsApp green)
z-index: 50
box-shadow: --shadow-sm
```

**Visível:** durante todo o scroll em mobile  
**Escondido:** em desktop (≥ 768px) — o botão do header substitui  
**Animação:** pulse subtil a cada 4 segundos se o utilizador estiver parado mais de 10 segundos na página (opcional, apenas se não activar `prefers-reduced-motion`)

**Posicionamento no DOM:** antes do `</body>`, fora da flow do conteúdo.

---

## Galerias — Comportamento Responsivo

### Grid de galeria

| Breakpoint | Colunas | Itens featured |
|---|---|---|
| `< 640px` | 1 | Ocupa 100% como os restantes |
| `640px–1023px` | 2 | Item featured: 2 colunas (full row) |
| `1024px+` | 3 | Item featured: 2 colunas (maior em destaque) |

### Ordem dos itens

A API retorna os itens com `featured: true` primeiro (`findByCompanyIdAndActiveTrueOrderByFeaturedDescDisplayOrderAsc`). A grelha CSS posiciona o primeiro item com `grid-column: span 2` em desktop para o destacar.

---

## Comparador Antes/Depois — Responsivo

### Mobile (< 768px)

- Imagens empilhadas verticalmente (ANTES / DEPOIS)
- Aspect-ratio: `4/3` por imagem
- Largura: 100%
- Label em cada imagem (canto superior)
- Navegação entre pares: dots de paginação + botões ← →
- **Sem slider interactivo** (custo de implementação elevado, experiência degradada em touch)

### Tablet (768px–1023px)

- Imagens lado a lado (ANTES | DEPOIS) em layout 50/50
- Sem slider drag — transição por botão
- Em portrait: empilhadas como mobile

### Desktop (1024px+)

- Slider interactivo com drag horizontal
- Handle central: 36px de diâmetro, com ícone ↔
- `cursor: col-resize` na zona de interacção
- Transição imediata (sem CSS transition na linha)

---

## Imagens — Comportamento Responsivo

**Estratégia:** `srcset` e `sizes` para servir imagens optimizadas por breakpoint.

```html
<!-- Exemplo conceptual, não código de implementação -->
<img
  srcset="foto-480.webp 480w, foto-768.webp 768w, foto-1280.webp 1280w"
  sizes="(max-width: 640px) 100vw,
         (max-width: 1024px) 50vw,
         40vw"
  alt="[descrição real da imagem]"
  loading="lazy"
/>
```

**Formatos:** WebP com fallback JPEG. Gerados no build ou servidos pelo Supabase Storage com transformação de imagem.

**Loading:**
- Hero: `loading="eager"` (acima do fold, crítico para LCP)
- Restantes: `loading="lazy"` com `decoding="async"`

**Aspect-ratio reservado:**
- Hero imagem: `aspect-ratio: 4/3` (mobile), `aspect-ratio: 3/4` ou `none` com `height: 100%` (desktop)
- Galeria: `aspect-ratio: 4/3` consistente
- Sobre: `aspect-ratio: 1/1` ou `3/2` dependendo da fotografia disponível

---

## Tipografia Responsiva

| Token | Mobile | Tablet | Desktop |
|---|---|---|---|
| `--text-display-2xl` | 2.5rem (40px) | 3.5rem (56px) | 4.5rem (72px) |
| `--text-display-xl` | 2rem (32px) | 2.5rem (40px) | 3.5rem (56px) |
| `--text-display-lg` | 1.75rem (28px) | 2rem (32px) | 2.5rem (40px) |
| `--text-display-md` | 1.5rem (24px) | 1.75rem (28px) | 1.875rem (30px) |
| `--text-lg` | 1.125rem (18px) | 1.125rem (18px) | 1.25rem (20px) |
| `--text-base` | 1rem (16px) | 1rem (16px) | 1rem (16px) |

Implementado com CSS `clamp()` para fluidity entre breakpoints:

```css
/* Exemplo conceptual */
--text-display-2xl: clamp(2.5rem, 5vw + 1rem, 4.5rem);
--text-display-xl:  clamp(2rem, 3vw + 1rem, 3.5rem);
```

---

## Navegação — Mobile

### Menu de Hamburguer

- Ícone: 3 linhas (24×18px, gap 6px), peso 2px
- Ao abrir: anima para ✕ (rotação 45° das linhas externas)
- Drawer: desliza da direita, 280px de largura
- Fundo: backdrop `rgba(0,0,0,0.5)`, z-index 40 (abaixo do drawer, z-index 41)
- Fechar: clique no backdrop, ✕, ESC ou foco fora do drawer

### Links da navegação em mobile

- Padding: mínimo `16px 24px` por link (área de toque 48px de altura)
- Sem hover no touch (usar `:active` em vez de `:hover`)

---

## Formulários — Comportamento Responsivo

### Mobile

- Campos em coluna única, full-width
- Altura mínima do input: 48px (área de toque)
- Teclado virtual: campos `type="tel"` para telefone abre teclado numérico
- `inputmode="tel"` para telemóvel
- Scroll automático para o campo com erro após submit
- O botão de submit ocupa a largura total em mobile

### Desktop

- Dois campos por linha onde faz sentido (Nome + Telefone)
- Dropdown de serviço: largura total
- Textarea: `min-height: 120px`, redimensionável verticalmente

---

## Áreas Tocáveis (Touch Targets)

Norma WCAG 2.5.5 (AAA) e 2.5.8 (AA, WCAG 2.2): área tocável mínima de 24×24px, mas recomendação prática de 44×44px.

| Elemento | Tamanho mínimo |
|---|---|
| Botões de CTA | 48px de altura |
| Links de navegação | 44px de área |
| Ícones clicáveis | 44×44px de área (com padding) |
| Handle do slider | 44×44px |
| Dots de paginação | 32×32px com gap de 8px |
| WhatsApp flutuante | 56×56px |
| Checkbox/radio | 24×24px visual + 44×44px de área |

---

## Performance em Rede Móvel

### Estratégia

1. **Imagens lazy-loaded** com placeholder de baixa resolução (blur up ou cor de background)
2. **Font loading:** `font-display: swap` para evitar FOIT (Flash of Invisible Text)
3. **Above the fold crítico:** inline CSS para header e hero (Critical CSS)
4. **Sem third-party scripts pesados** na landing (Google Analytics deve ser diferido)
5. **Mapa (se existir):** `loading="lazy"` e carregado apenas no intersection do viewport

### Metas de performance

| Métrica | Meta |
|---|---|
| LCP (Largest Contentful Paint) | < 2.5s em 4G |
| CLS (Cumulative Layout Shift) | < 0.1 |
| FID / INP | < 200ms |
| Total de imagens above-the-fold | < 200KB (compressed) |
| Total de JS no carregamento inicial | < 100KB (gzipped) |

### Imagens de hero

- Servir versão 480w para mobile (< 640px)
- Servir versão 1280w para desktop
- Formato: WebP com compressão quality=85
- Placeholder: cor de fundo `--surface-muted` enquanto carrega (sem spinner)

---

## Sumário de Decisões por Componente

| Componente | Mobile | Tablet | Desktop |
|---|---|---|---|
| Header | 56px, hamburguer, WhatsApp ícone | 64px, links parciais, WhatsApp btn | 72px, todos os links, WhatsApp btn |
| Hero | Imagem topo, texto abaixo, 1 coluna | Imagem direita, texto esquerda | Imagem direita (45%), texto (55%) |
| Estatísticas | 2×2 grid | 4 em linha | 4 em linha |
| Serviços | 1 coluna, lista | 2 colunas | 3 colunas |
| Galeria | 1 coluna, empilhado | 2 colunas | 3 colunas, featured span 2 |
| Antes/depois | Empilhado, sem slider | Lado a lado, sem slider | Slider interactivo |
| Processo | 1 coluna, numerado | 2×2 grid | 4 em linha |
| Sobre | Imagem topo, texto abaixo | 2 colunas 50/50 | 2 colunas 45/55 |
| CTA/Contacto | WhatsApp btn full-width, form 1 col | Form 2 colunas | Form 2 colunas |
| Footer | 1 coluna, compacto | 2 colunas | 3 colunas |
| WhatsApp fixo | Visível, bottom-right | Visível | Escondido |
