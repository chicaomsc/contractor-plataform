# Wireframes da Landing Page — JR Pinturas

**Sprint:** 7A — Design Direction  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Referência:** [content-structure.md](./content-structure.md) | [design-system.md](./design-system.md)

---

## Convenções dos Wireframes

```
█████  Imagem / fotografia
░░░░░  Fundo alternado (surface-muted)
─────  Linha separadora / borda
▓▓▓▓▓  Botão primário (laranja)
▒▒▒▒▒  Botão secundário (outline)
[   ]  Input / campo de formulário
(   )  Select / dropdown
```

---

## Comportamento de Navegação

### Scroll behavior
- Header fixo com `position: sticky; top: 0`
- Em mobile: logo à esquerda, ícone WhatsApp à direita, hamburguer ao centro-direito
- Em desktop: logo à esquerda, links de navegação ao centro, botão WhatsApp à direita
- Ao scroll para baixo: header reduz altura de 72px para 56px com transição suave
- Ao scroll para cima: header restaura (não implementar se complexo — scroll down hidden também é válido)

### WhatsApp fixo em mobile
- Botão circular flutuante no canto inferior direito
- `position: fixed; bottom: 20px; right: 20px; z-index: 50`
- Visível em toda a página excepto quando o header está em foco de navegação
- **Não** aparece em desktop (o header já tem o botão)

---

## MOBILE (375px → 767px)

```
┌─────────────────────────┐  ← 375px
│ ▣ JR PINTURAS    ☰  📱 │  ← Header fixo, 56px
│─────────────────────────│
│                         │
│  ████████████████████   │  ← Hero fotografia, 240px
│  ████████████████████   │    (100% largura, object-fit: cover)
│  ████████████████████   │
│  ████████████████████   │
│                         │
│  Pinturas e remodelaçõ- │  ← h1, Barlow Bold, 32px, foreground
│  ões em [Cidade].       │
│                         │
│  Orçamento em 24h, obra │  ← Lead text, 16px, muted-foreground
│  com prazo garantido.   │
│                         │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│  ← CTA primário, laranja, largura total
│   Pedir orçamento       │
│  ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒│  ← CTA secundário, outline
│   Ver os nossos traba.. │
│                         │
│─────────────────────────│
│                         │
│  ░░░░░░░░░░░░░░░░░░░░░  │  ← Barra de confiança, surface-dark
│  ░                   ░  │
│  ░  8     150+      ░  │
│  ░  anos  obras     ░  │
│  ░                   ░  │
│  ░  200+   4.9/5    ░  │
│  ░  clien  avalia.  ░  │
│  ░                   ░  │
│  ░░░░░░░░░░░░░░░░░░░░░  │
│                         │
│─────────────────────────│
│                         │
│  O QUE FAZEMOS          │  ← Section label, uppercase, xs, primary
│                         │
│  Pintura residencial    │  ← Serviço, bold, 18px
│  Renovação interior e   │  ← Descrição, 14px, muted
│  exterior de habitações │
│  ─────────────────────  │
│                         │
│  Pintura comercial      │
│  Espaços de trabalho e  │
│  estabelecimentos       │
│  ─────────────────────  │
│                         │
│  Ladrilhos e mosaicos   │
│  ...                    │
│  ─────────────────────  │
│                         │
│  [continua para todos   │
│   os serviços...]       │
│                         │
│─────────────────────────│
│                         │
│  ANTES E DEPOIS         │  ← Section label
│                         │
│  ████████████████████   │  ← Imagem ANTES, 100% largura
│  ████████████████████   │    label "ANTES" no canto
│  ████████████████████   │
│  [  ANTES  ]            │  ← Label sobrep., fundo carvão 60%
│                         │
│  ████████████████████   │  ← Imagem DEPOIS, 100% largura
│  ████████████████████   │
│  ████████████████████   │
│  [  DEPOIS ]            │  ← Label sobrep., fundo laranja 60%
│                         │
│  ← ─────────────── →   │  ← Navegação entre pares (dots/arrows)
│       ○ ● ○ ○          │
│                         │
│─────────────────────────│
│                         │
│  COMO TRABALHAMOS       │  ← Section label
│                         │
│  ① Contacto             │  ← Número grande (40px), bold
│  WhatsApp ou formulário │
│  em qualquer altura     │
│                         │
│  ② Visita               │
│  Vamos ao local, sem    │
│  compromisso, para ver  │
│  o que é preciso        │
│                         │
│  ③ Orçamento            │
│  Detalhado e sem        │
│  surpresas, em 24–48h   │
│                         │
│  ④ Obra                 │
│  Com prazo acordado e   │
│  limpeza garantida no   │
│  final                  │
│                         │
│─────────────────────────│
│                         │
│  ████████████████████   │  ← Foto equipa/fundador
│  ████████████████████   │
│  ████████████████████   │
│                         │
│  SOBRE A EMPRESA        │  ← Section label
│                         │
│  A JR Pinturas foi      │  ← Texto institucional
│  fundada em [ano]...    │
│  [2–3 parágrafos]       │
│                         │
│─────────────────────────│
│                         │
│  ONDE TRABALHAMOS       │  ← Section label
│                         │
│  Concelhos de [área]:   │
│  · [Concelho A]         │
│  · [Concelho B]         │
│  · [Concelho C]         │
│  · [+ outros]           │
│                         │
│  Fora desta área?       │  ← Link/texto
│  Pergunte-nos →         │
│                         │
│─────────────────────────│
│                         │
│  ░░░░░░░░░░░░░░░░░░░░░  │  ← CTA final, surface-dark
│  ░                   ░  │
│  ░  PEÇA O SEU       ░  │
│  ░  ORÇAMENTO        ░  │
│  ░  GRATUITO         ░  │
│  ░                   ░  │
│  ░  Resposta em 24h  ░  │
│  ░                   ░  │
│  ░  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ░  │  ← WhatsApp CTA (verde)
│  ░  📱 WhatsApp      ░  │
│  ░                   ░  │
│  ░  ─── ou ──────── ░  │
│  ░                   ░  │
│  ░  [ Nome        ] ░  │
│  ░  [ Telefone    ] ░  │
│  ░  ( Serviço   ▼) ░  │
│  ░  [ Mensagem    ] ░  │
│  ░                   ░  │
│  ░  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ░  │
│  ░  Enviar mensagem  ░  │
│  ░                   ░  │
│  ░░░░░░░░░░░░░░░░░░░░░  │
│                         │
│─────────────────────────│
│                         │
│  ▣ JR PINTURAS          │  ← Footer
│                         │
│  Serviços Galeria       │
│  Como funciona          │
│  Privacidade  Termos    │
│                         │
│  [ig] [fb]              │  ← Redes sociais (se existirem)
│                         │
│  © 2026 JR Pinturas     │
│  Todos os direitos res. │
│                         │
└─────────────────────────┘

                  [📱]     ← WhatsApp flutuante fixo, bottom-right
```

---

## TABLET (768px → 1023px)

```
┌───────────────────────────────────────┐  ← 768px
│ ▣ JR PINTURAS   Serviç Gal. Cont.  📱│  ← Header, 64px
│───────────────────────────────────────│
│                                       │
│  ┌─────────────────┐  ┌─────────────┐ │  ← Hero: 2 colunas
│  │                 │  │████████████│ │
│  │ Pinturas e      │  │████████████│ │  ← Foto 45% largura
│  │ remodelações    │  │████████████│ │
│  │ em [Cidade]     │  │████████████│ │
│  │                 │  │████████████│ │
│  │ Lead text       │  └─────────────┘ │
│  │ descritivo...   │                  │
│  │                 │                  │
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │
│  │  Pedir orçam.  │                  │
│  │ ▒▒▒▒▒▒▒▒▒▒▒▒▒ │                  │
│  │  Ver trabalhos │                  │
│  └─────────────────┘                  │
│                                       │
│───────────────────────────────────────│
│                                       │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │  ← Barra confiança, dark
│  ░  [8 anos] [150+ obras] [200+ cl] ░│
│  ░  [Avaliação 4.9/5               ]░│
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                       │
│───────────────────────────────────────│
│                                       │
│  O QUE FAZEMOS                        │
│                                       │
│  ┌──────────────────┐  ┌────────────┐ │  ← 2 colunas de serviços
│  │ Pintura          │  │ Pintura    │ │
│  │ residencial      │  │ comercial  │ │
│  │ Descrição curta  │  │ Descrição  │ │
│  └──────────────────┘  └────────────┘ │
│  ┌──────────────────┐  ┌────────────┐ │
│  │ Ladrilhos        │  │ Demolições │ │
│  │ ...              │  │ ...        │ │
│  └──────────────────┘  └────────────┘ │
│  [continua...]                        │
│                                       │
│───────────────────────────────────────│
│                                       │
│  ANTES E DEPOIS                       │
│                                       │
│  ┌────────────────────────────────────┐│  ← Slider full-width
│  │ ████████████│████████████████████ ││  ← Divisória vertical
│  │ ████ANTES██│██████████DEPOIS█████ ││
│  │ ████████████│████████████████████ ││
│  │ ████████████│████████████████████ ││
│  └────────────────────────────────────┘│
│       ○ ● ○ ○ ○                        │
│                                        │
│───────────────────────────────────────│
│                                       │
│  COMO TRABALHAMOS                     │
│                                       │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐│
│  │ ①      │  │ ②      │  │ ③      │  │ ④      ││
│  │Contact │  │ Visita │  │ Orçam. │  │ Obra   ││
│  │        │  │        │  │        │  │        ││
│  │ texto  │  │ texto  │  │ texto  │  │ texto  ││
│  └────────┘  └────────┘  └────────┘  └────────┘│
│                                       │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                │
│   Começar agora                       │
│                                       │
│───────────────────────────────────────│
│                                       │
│  ┌──────────────────┐  ┌────────────┐ │  ← Sobre: foto + texto
│  │████████████████ │  │ SOBRE NÓS  │ │
│  │████████████████ │  │            │ │
│  │████████████████ │  │ A JR       │ │
│  │████████████████ │  │ Pinturas...│ │
│  │████████████████ │  │            │ │
│  └──────────────────┘  └────────────┘ │
│                                       │
│───────────────────────────────────────│
│                                       │
│  ONDE TRABALHAMOS                     │
│                                       │
│  [Mapa simplificado opcional]  [Lista]│
│  · Concelho A · Concelho B           │
│  · Concelho C · Concelho D           │
│                                       │
│───────────────────────────────────────│
│                                       │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │  ← CTA final
│  ░  PEÇA O SEU ORÇAMENTO GRATUITO  ░  │
│  ░                                 ░  │
│  ░  ┌──────────────┐ ┌──────────┐ ░  │
│  ░  │ Nome         │ │ Telefone │ ░  │
│  ░  └──────────────┘ └──────────┘ ░  │
│  ░  ┌────────────────────────────┐ ░  │
│  ░  │ Serviço                  ▼│ ░  │
│  ░  └────────────────────────────┘ ░  │
│  ░  ┌────────────────────────────┐ ░  │
│  ░  │ Mensagem                   │ ░  │
│  ░  │                            │ ░  │
│  ░  └────────────────────────────┘ ░  │
│  ░  ▓▓▓▓▓▓▓▓▓▓  📱 WhatsApp       ░  │
│  ░  ▓▓▓▓▓▓▓▓▓▓  Enviar mensagem   ░  │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                       │
│ [Logo] [Serviços Galeria Privacidade] │  ← Footer
│ © 2026 JR Pinturas                   │
└───────────────────────────────────────┘
```

---

## DESKTOP (1024px → 1280px+)

```
┌──────────────────────────────────────────────────────────────┐  ← 1280px
│ ▣ JR PINTURAS    Serviços  Galeria  Como funciona  Contacto  │ [📱 WhatsApp] │
│  fixed, 72px                                                  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  container max 1280px, px 64px                               │
│  ┌────────────────────────┐   ┌──────────────────────────┐   │
│  │                        │   │██████████████████████████│   │
│  │                        │   │██████████████████████████│   │
│  │  ─────                 │   │██████████████████████████│   │  ← Linha laranja 3px
│  │  Pinturas e remodelações   │██████████████████████████│   │
│  │  em [Cidade].          │   │██████████████████████████│   │
│  │                        │   │██████████████████████████│   │
│  │  (h1, 56–72px, Barlow  │   │██████████████████████████│   │  ← 55% / 45%
│  │   Bold, tight leading) │   │██████████████████████████│   │
│  │                        │   │██████████████████████████│   │
│  │  Orçamento em 24 horas,│   │██████████████████████████│   │
│  │  obra com prazo e      │   │██████████████████████████│   │
│  │  limpeza garantidos.   │   └──────────────────────────┘   │
│  │                        │    Detalhe: borda esq. laranja    │
│  │  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │   nos 4px do lado esquerdo       │
│  │   Pedir orçamento gra- │    da foto                        │
│  │   tuito                │                                   │
│  │                        │                                   │
│  │  ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒  │                                   │
│  │   Ver os nossos traba- │                                   │
│  │   lhos                 │                                   │
│  └────────────────────────┘                                   │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  ░   [8 anos de        │  150+ obras      │  200+ clientes] ░│  ← Dark section
│  ░   experiência]      │  concluídas      │  satisfeitos   ░│
│  ░                    ─┼─               ─┼─               ░│
│  ░                     │                 │  [Avaliação 4.9]░│
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  O QUE FAZEMOS                                               │
│                                                              │
│  ┌───────────────────┐  ┌───────────────────┐  ┌──────────┐ │
│  │ Pintura           │  │ Pintura           │  │ Ladrilhos│ │
│  │ residencial       │  │ comercial         │  │          │ │
│  │                   │  │                   │  │          │ │
│  │ Renovação inte-   │  │ Espaços empresa-  │  │ Cerâmica │ │
│  │ rior e exterior   │  │ riais e comerciais│  │ e mosaico│ │
│  └───────────────────┘  └───────────────────┘  └──────────┘ │
│  (linha separadora, não card com sombra)                     │
│  ┌───────────────────┐  ┌───────────────────┐  ┌──────────┐ │
│  │ Demolições        │  │ Pavimentação      │  │ Remodelaç│ │
│  │                   │  │                   │  │          │ │
│  └───────────────────┘  └───────────────────┘  └──────────┘ │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ANTES E DEPOIS          [← navegar →]                       │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ ████████████████████│████████████████████████████████│ │  │  ← Slider
│  │ █ ANTES ████████████│████████████████████ DEPOIS ████│ │  │    full-width
│  │ ████████████████████│████████████████████████████████│ │  │    slider
│  │ ████████████████████│████████████████████████████████│ │  │    interactivo
│  │ ████████████████████│████████████████████████████████│ │  │
│  │ ████████████████████│████████████████████████████████│ │  │
│  └────────────────────────────────────────────────────────┘  │
│  Título da obra            ○ ● ○ ○ ○                         │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  ░  COMO TRABALHAMOS                                      ░  │
│  ░                                                        ░  │
│  ░   ①               ②               ③               ④   ░  │
│  ░   Contacto        Visita          Orçamento       Obra  ░  │
│  ░   ───────         ──────          ─────────       ────  ░  │
│  ░   WhatsApp ou     Vamos ao        Detalhado,      Com   ░  │
│  ░   formulário,     local, sem      sem surpresas,  prazo ░  │
│  ░   em qualquer     compromisso     em 24–48h       e     ░  │
│  ░   altura          para avaliar                    limpe-░  │
│  ░                   o trabalho                      za    ░  │
│  ░                                                        ░  │
│  ░               ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓               ░  │
│  ░                  Começar agora                         ░  │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ┌──────────────────────────┐  ┌───────────────────────────┐ │
│  │██████████████████████████│  │ SOBRE NÓS                │ │
│  │██████████████████████████│  │                           │ │
│  │██████████████████████████│  │ A JR Pinturas foi fundada│ │
│  │██████████████████████████│  │ em [ano] por [nome].     │ │
│  │██████████████████████████│  │                           │ │
│  │██████████████████████████│  │ Trabalhamos com ...       │ │
│  │██████████████████████████│  │                           │ │
│  └──────────────────────────┘  │ [parágrafo 2]             │ │
│                                └───────────────────────────┘ │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ONDE TRABALHAMOS                                            │
│                                                              │
│  ┌──────────────────────────────┐  ┌────────────────────┐   │
│  │  Actuamos nos concelhos de:  │  │ · Concelho A       │   │
│  │                              │  │ · Concelho B       │   │
│  │  [Zona geográfica em texto   │  │ · Concelho C       │   │
│  │   ou mapa simplificado]      │  │ · Concelho D       │   │
│  │                              │  │ · + outros         │   │
│  └──────────────────────────────┘  └────────────────────┘   │
│                                                              │
│  Fora desta área? Contacte-nos e vemos se conseguimos ajudar.│
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  ░                                                        ░  │
│  ░  PEÇA O SEU ORÇAMENTO GRATUITO                        ░  │
│  ░  Respondemos em menos de 24 horas                     ░  │
│  ░                                                        ░  │
│  ░  ┌──────────────────────┐  ┌──────────────────────┐  ░  │
│  ░  │                      │  │                      │  ░  │
│  ░  │ [Nome              ] │  │ [Telefone          ] │  ░  │
│  ░  │                      │  │                      │  ░  │
│  ░  └──────────────────────┘  └──────────────────────┘  ░  │
│  ░  ┌──────────────────────────────────────────────────┐ ░  │
│  ░  │ (Serviço de interesse                          ▼)│ ░  │
│  ░  └──────────────────────────────────────────────────┘ ░  │
│  ░  ┌──────────────────────────────────────────────────┐ ░  │
│  ░  │ Mensagem (opcional)                              │ ░  │
│  ░  │                                                  │ ░  │
│  ░  └──────────────────────────────────────────────────┘ ░  │
│  ░                                                        ░  │
│  ░  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓            ░  │
│  ░  📱 Enviar WhatsApp    ✉ Enviar formulário          ░  │
│  ░                                                        ░  │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ▣ JR PINTURAS        Serviços  Galeria  Privacidade  Termos │
│                       © 2026 JR Pinturas. Todos os direitos. │
│                                            [ig] [fb]         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Comparador Antes/Depois — Especificação Detalhada

### Modo Desktop (slider interactivo)

- Imagem ANTES à esquerda, imagem DEPOIS à direita
- Linha divisória vertical: 1px, cor `--border`, handle circular 36px (ícone de seta bidireccional)
- Drag: arrastar a linha horizontal para revelar mais de um lado
- Cursor: `col-resize` na linha; `grab`/`grabbing` no handle
- Posição inicial: 50% (divisão igual)
- Transição da linha: `transform` sem `transition` (segue o ponteiro imediatamente)
- Sem border-radius nas imagens
- Label "ANTES" no canto superior esquerdo: background `--foreground 80%`, texto branco, xs, uppercase, padding 4px 8px
- Label "DEPOIS" no canto superior direito: background `--primary 80%`, texto branco
- Número de itens: mostra todos os itens com `featured: true` da API; navegar entre pares com botões ← →

### Modo Mobile (empilhado)

- Duas imagens em coluna: ANTES em cima, DEPOIS em baixo
- Cada imagem: 100% largura, aspect-ratio 4/3
- Label no canto de cada imagem (mesmo tratamento do desktop)
- Navegar entre pares: swipe horizontal na secção inteira, OU dots de paginação
- Sem slider interactivo em mobile (custo de implementação alto, benefício baixo em ecrã pequeno)

### Estado sem imagens

```
┌──────────────────────────────────────────┐
│                                          │
│         ⬜ Fotografia em breve           │
│    As nossas obras recentes estarão      │
│         disponíveis em breve.            │
│                                          │
│      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓            │
│         Ver outros trabalhos             │
│                                          │
└──────────────────────────────────────────┘
```

**Nunca:** secção vazia, placeholder de quebra de layout, ícone genérico de câmara.

---

## Estados Sem Conteúdo (Empty States)

### Sem serviços

```
┌──────────────────────────────────────────┐
│                                          │
│   O QUE FAZEMOS                         │
│                                          │
│   Estamos a preparar a nossa lista       │
│   de serviços.                           │
│                                          │
│   Entretanto, contacte-nos pelo          │
│   WhatsApp para saber mais.              │
│                                          │
│   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                    │
│   📱 WhatsApp                           │
│                                          │
└──────────────────────────────────────────┘
```

### Sem galeria

- Secção inteira **ocultada** — não exibir com placeholders
- O fluxo salta directamente de Serviços para Processo

---

## Hierarquia do Hero — Ordem de Leitura

Em ambos os formatos (mobile e desktop), a ordem de leitura do hero segue:

1. **Fotografia** (impacto visual imediato — "onde estou?")
2. **Headline** (promessa concreta — "o que fazem?")
3. **Subheadline** (prova rápida — "porquê eles?")
4. **CTA primário** (acção — "pedir orçamento")
5. **CTA secundário** (curiosidade — "ver trabalhos")

**Em mobile:** a fotografia aparece ANTES do texto (posição física no DOM). Em desktop, ficam lado a lado mas o olho lê a fotografia primeiro por peso visual.

---

## Rodapé — Layout

```
MOBILE                          DESKTOP
────────────────────────        ────────────────────────────────────────────
[Logo]                          [Logo]    [Nav links]         [Redes sociais]
                                
Serviços                        © 2026 JR Pinturas · Privacidade · Termos
Galeria
Como funciona
Privacidade
Termos

[ig] [fb]

© 2026 JR Pinturas
```

**Altura:** mínima, sem conteúdo de marketing. Footer não converte — é referência legal.
