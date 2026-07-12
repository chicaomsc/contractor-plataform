# Inventário de Componentes — Contractor Platform

**Sprint:** 7A — Design Direction  
**Versão:** 1.1  
**Data:** 2026-07-12  
**Estado:** Especificação pré-implementação — nenhum código criado

---

> Este documento lista e especifica componentes futuros. Nenhum código foi escrito.  
> A implementação ocorre nas Sprints 7B e 7C.

---

## Princípio Fundamental: Componentes Genéricos

**Todos os componentes deste inventário são genéricos. Nenhum conhece a JR Pinturas.**

A landing page pertence ao Contractor Platform. JR Pinturas é o primeiro tenant, não o único. Todo o branding, conteúdo e dados são carregados da API pública em runtime.

**Nomes proibidos:**

| Errado | Correcto |
|---|---|
| `JRHeroSection` | `HeroSection` |
| `JRServiceList` | `ServiceSection` |
| `JRGallery` | `BeforeAfterSection` |
| `PinturasContactForm` | `ContactSection` |

A regra: se o nome do componente inclui o nome de uma empresa ou sector, está errado.

**Nomes permitidos:**

Genéricos descritivos (`HeroSection`, `ServiceSection`, `GalleryCarousel`, `ProcessSection`) ou atómicos (`Button`, `FormField`, `EmptyState`).

Ver [component-architecture.md](component-architecture.md) para a hierarquia completa e regras de naming.

---

## Convenções

**Server Component:** Renderizado no servidor (Next.js RSC). Sem state, sem event handlers. Recebe dados via props ou fetch directo.  
**Client Component:** Tem `"use client"`. Usa `useState`, `useEffect`, event handlers, browser APIs.  
**Híbrido:** Componente pai é Server Component; partes interactivas são Client Components filhos.

---

## LAYOUT

---

### `PageLayout`

**Responsabilidade:** Wrapper raiz da landing. Fornece a estrutura HTML semântica (html, body, header, main, footer), aplica as variáveis de CSS do design system, inclui o skip link.

**Variações:** nenhuma — é único por página.

**Propriedades essenciais:**
- `company: CompanyPublicData` — dados da empresa (nome, logo, cores) para configurar o branding
- `children: ReactNode`

**Server ou Client:** Server Component.

**Acessibilidade:**
- Inclui `<html lang="pt">`
- `<a href="#main-content">Saltar para o conteúdo</a>` como primeiro elemento do DOM
- `<main id="main-content">`
- `<header role="banner">` e `<footer role="contentinfo">`

**Dependências:** `SiteHeader`, `SiteFooter`, `WhatsAppFAB`

---

### `Section`

**Responsabilidade:** Wrapper semântico para cada secção da landing. Gere padding, fundo alternado e identificador de âncora.

**Variações:**
- `default` — fundo `--background`
- `muted` — fundo `--surface-muted`
- `dark` — fundo `--surface-dark`, texto `--surface-dark-fg`

**Propriedades essenciais:**
- `id?: string` — âncora de navegação
- `variant?: 'default' | 'muted' | 'dark'`
- `aria-labelledby?: string` — ID do heading da secção
- `children: ReactNode`

**Server ou Client:** Server Component.

**Acessibilidade:** `<section aria-labelledby={...}>` — heading obrigatório dentro.

**Dependências:** nenhuma.

---

### `Container`

**Responsabilidade:** Centra o conteúdo e aplica max-width e padding horizontal responsivo.

**Variações:**
- `wide` — max-width 1280px
- `narrow` — max-width 768px (para texto longo)

**Propriedades essenciais:**
- `size?: 'wide' | 'narrow'` (default: `wide`)
- `children: ReactNode`

**Server ou Client:** Server Component.

**Acessibilidade:** elemento neutro (`<div>`).

---

## NAVIGATION

---

### `SiteHeader`

**Responsabilidade:** Header fixo com logo, links de navegação e CTA de WhatsApp. Comprime ao scroll.

**Variações:**
- Estado normal (alto: 72px desktop)
- Estado comprimido ao scroll (56px)
- Menu aberto (mobile)

**Propriedades essenciais:**
- `logoUrl: string`
- `companyName: string`
- `whatsappNumber: string` — para link `wa.me/...`
- `navLinks: Array<{ label: string; href: string }>` — gerados pela página

**Server ou Client:** Híbrido. O wrapper é Server; o scroll behavior e o menu mobile são Client.

**Acessibilidade:**
- `<header role="banner">`
- `<nav aria-label="Navegação principal">`
- `<button aria-expanded aria-controls="nav-drawer">` no hamburguer
- `aria-hidden="true"` no drawer quando fechado
- ESC fecha o menu

**Dependências:** `NavDrawer` (client component para mobile)

---

### `NavDrawer`

**Responsabilidade:** Menu lateral que desliza em mobile quando o hamburguer é activado.

**Variações:** nenhuma.

**Propriedades essenciais:**
- `isOpen: boolean`
- `onClose: () => void`
- `links: Array<{ label: string; href: string }>`

**Server ou Client:** Client Component (`useState`, `useEffect` para trap de foco).

**Acessibilidade:**
- `role="dialog"` ou `role="navigation"`
- `aria-modal="true"` se for tratado como dialog
- Trap de foco enquanto aberto
- Fechar com ESC
- Scroll da página bloqueado enquanto aberto (`body overflow: hidden`)
- Focus restaurado ao botão hamburguer ao fechar

**Dependências:** nenhuma.

---

### `WhatsAppFAB`

**Responsabilidade:** Botão circular flutuante de WhatsApp em mobile. Fixo no canto inferior direito.

**Variações:** nenhuma (apenas visível em mobile).

**Propriedades essenciais:**
- `whatsappNumber: string`
- `message?: string` — mensagem pré-preenchida (ex: "Olá, gostaria de pedir um orçamento")

**Server ou Client:** Client Component (pode ter animação de pulse via CSS, mas o comportamento de visibilidade depende do viewport — pode ser CSS only com `@media`).

**Acessibilidade:**
- `<a href="https://wa.me/...">` — link, não botão (navega para app externa)
- `aria-label="Contactar pelo WhatsApp"`
- `target="_blank"` com `rel="noopener noreferrer"`
- `<span class="sr-only">Abre o WhatsApp (nova janela)</span>`

**Dependências:** nenhuma.

---

## MARKETING

---

### `HeroSection`

**Responsabilidade:** Primeira secção visível da página. Promessa principal, fotografia de obra, CTAs.

**Variações:**
- `with-photo` — layout dividido (texto + imagem)
- `no-photo` — layout centrado com fundo de textura (fallback sem fotografia)

**Propriedades essenciais:**
- `headline: string` — título principal (h1)
- `subheadline: string` — texto de apoio
- `primaryCta: { label: string; href: string }`
- `secondaryCta?: { label: string; href: string }`
- `heroImageUrl?: string` — URL da imagem da obra
- `heroImageAlt?: string` — descrição para acessibilidade

**Server ou Client:** Server Component.

**Acessibilidade:**
- `<h1>` para o headline
- Imagem com `alt` descritivo
- `loading="eager"` na imagem hero (LCP crítico)

**Dependências:** `Button`, `Section`, `Container`

---

### `TrustStrip`

**Responsabilidade:** Barra de métricas de credibilidade (anos, obras, clientes, avaliação).

**Variações:**
- `static` — valores fixos sem animação
- `animated` — count-up ao entrar no viewport (respeitando `prefers-reduced-motion`)

**Propriedades essenciais:**
- `metrics: Array<{ value: string | number; label: string; suffix?: string }>`

**Server ou Client:** Híbrido. Base é Server; count-up é Client (Intersection Observer).

**Acessibilidade:**
- `<h2 class="sr-only">Experiência e credenciais</h2>` (heading invisível para leitores de ecrã)
- `<dl>` com `<dt>` (label) e `<dd>` (valor) para estrutura semântica das métricas

**Dependências:** nenhuma.

---

### `ServiceList`

**Responsabilidade:** Lista de serviços activos da empresa, consumindo a API pública.

**Variações:**
- `list` — layout de lista vertical (mobile)
- `grid` — grelha de 2–3 colunas (tablet/desktop)
- `empty` — estado vazio com CTA de WhatsApp

**Propriedades essenciais:**
- `services: PublicServiceResponse[]` — dados da API
- `layout?: 'list' | 'grid'`

**Server ou Client:** Server Component (dados fetched no servidor).

**Acessibilidade:**
- `<h2>O que fazemos</h2>`
- `<ul>` com `<li>` por serviço
- Se existir link "Saber mais": `aria-label="Saber mais sobre Pintura residencial"`

**Dependências:** `ServiceItem`

---

### `ServiceItem`

**Responsabilidade:** Um item individual de serviço.

**Variações:** nenhuma (apenas layout responsivo via CSS container queries ou classe).

**Propriedades essenciais:**
- `service: PublicServiceResponse` — `{ name, shortDescription, icon?, slug }`

**Server ou Client:** Server Component.

**Acessibilidade:**
- `<li>` semântico
- Nome do serviço em `<h3>` ou `<strong>` (dependendo da hierarquia)

**Dependências:** nenhuma.

---

### `ProcessSteps`

**Responsabilidade:** Secção visual de 4 passos do processo de trabalho.

**Variações:** nenhuma.

**Propriedades essenciais:**
- `steps: Array<{ number: number; title: string; description: string }>`

**Server ou Client:** Server Component (conteúdo estático).

**Acessibilidade:**
- `<h2>Como trabalhamos</h2>`
- `<ol>` com `<li>` por passo (lista ordenada — a ordem é significativa)
- Números decorativos com `aria-hidden="true"` se o `<li>` já os incluir semanticamente

**Dependências:** nenhuma.

---

### `AboutSection`

**Responsabilidade:** Secção "Sobre nós" com texto e fotografia da equipa/fundador.

**Variações:**
- `with-photo` — layout 2 colunas
- `text-only` — layout 1 coluna (fallback sem fotografia)

**Propriedades essenciais:**
- `content: string` — texto HTML ou markdown processado
- `photoUrl?: string`
- `photoAlt?: string`

**Server ou Client:** Server Component.

**Acessibilidade:**
- `<h2>Sobre nós</h2>`
- Imagem com `alt` descritivo (não "foto da equipa" genérico)

**Dependências:** nenhuma.

---

### `ServiceAreaSection`

**Responsabilidade:** Lista de concelhos e zonas de actuação.

**Variações:** nenhuma.

**Propriedades essenciais:**
- `areas: string[]` — lista de concelhos
- `outOfAreaMessage?: string`
- `whatsappNumber: string` — para link de "Perguntar disponibilidade"

**Server ou Client:** Server Component.

**Acessibilidade:**
- `<h2>Onde trabalhamos</h2>`
- `<ul>` com `<li>` por área

**Dependências:** nenhuma.

---

### `WhatsAppCTA`

**Responsabilidade:** Secção de CTA final com formulário e botão WhatsApp. A secção de maior intenção de conversão.

**Variações:**
- `full` — WhatsApp + formulário
- `minimal` — apenas botão WhatsApp (para posições intermédias na página)

**Propriedades essenciais:**
- `whatsappNumber: string`
- `whatsappMessage?: string`
- `services: PublicServiceResponse[]` — para popular o dropdown
- `onSubmit?: (data: ContactFormData) => Promise<void>` — acção de submit

**Server ou Client:** Híbrido. Estrutura é Server; formulário e interacção é Client.

**Acessibilidade:**
- `<h2>Pedir orçamento</h2>`
- `<form aria-label="Formulário de pedido de orçamento">`
- Todos os campos com `<label>` associada
- `aria-required="true"` em campos obrigatórios
- `aria-describedby` para mensagens de erro
- `aria-busy="true"` no botão durante submit

**Dependências:** `ContactForm`, `Button`

---

## GALLERY

---

### `ProjectGallery`

**Responsabilidade:** Grelha de projectos de galeria. Exibe items com `featured` primeiro, num layout irregular.

**Variações:**
- `grid` — grelha com item featured em destaque
- `empty` — estado sem conteúdo (secção ocultada)

**Propriedades essenciais:**
- `items: PublicGalleryResponse[]`

**Server ou Client:** Server Component (dados fetched no servidor).

**Acessibilidade:**
- `<h2>Antes e depois</h2>`
- `<ul role="list">` com `<li>` por projecto

**Dependências:** `BeforeAfterComparison`

---

### `BeforeAfterComparison`

**Responsabilidade:** Componente de comparação antes/depois. Em desktop: slider interactivo. Em mobile: imagens empilhadas.

**Variações:**
- `slider` — desktop, drag interactivo
- `stacked` — mobile, imagens em coluna
- `side-by-side` — tablet, imagens lado a lado sem drag

**Propriedades essenciais:**
- `title: string` — título do projecto
- `beforeImageUrl: string`
- `beforeImageAlt: string`
- `afterImageUrl: string`
- `afterImageAlt: string`
- `initialPosition?: number` — posição inicial do slider (0–100, default 50)

**Server ou Client:** Client Component (requer `useState` para posição do slider, `onPointerMove`, `onKeyDown`).

**Acessibilidade:**
- `role="group"` com `aria-labelledby` apontando para o `<h3>` do projecto
- Handle com `role="slider"`, `aria-valuemin`, `aria-valuemax`, `aria-valuenow`, `aria-valuetext`
- Suporte total a teclado: ← → movem em 5%; Home/End vão para extremos
- Em modo empilhado: `<figure>` + `<figcaption>` para cada imagem

**Dependências:** nenhuma.

---

## FORMS

---

### `ContactForm`

**Responsabilidade:** Formulário de pedido de orçamento alternativo ao WhatsApp.

**Variações:**
- `full` — todos os campos
- `minimal` — apenas nome e telefone (para posições secundárias)

**Propriedades essenciais:**
- `services: Array<{ id: string; name: string }>` — para dropdown
- `onSubmit: (data: ContactFormData) => Promise<void>`

**Server ou Client:** Client Component (estado de formulário, validação, submit async).

**Acessibilidade:**
- `<form>` com `aria-label`
- `<fieldset>` e `<legend>` se houver grupos de campos
- `required` + `aria-required="true"` em campos obrigatórios
- `aria-invalid="true"` + `aria-describedby` em campos com erro
- `role="alert"` na mensagem de erro de submit
- `autocomplete` apropriado em todos os campos

**Dependências:** `FormField`, `Button`

---

### `FormField`

**Responsabilidade:** Campo de formulário atómico com label, input e mensagem de erro.

**Variações:**
- `text` — input text
- `tel` — input tel
- `email` — input email
- `textarea` — textarea
- `select` — select com opções

**Propriedades essenciais:**
- `id: string`
- `label: string`
- `type: 'text' | 'tel' | 'email' | 'textarea' | 'select'`
- `required?: boolean`
- `error?: string` — mensagem de erro
- `options?: Array<{ value: string; label: string }>` — para select
- `...htmlInputProps`

**Server ou Client:** Client Component (estado de erro local, validação ao blur).

**Acessibilidade:**
- `<label for={id}>`
- `aria-required` em campos obrigatórios
- `aria-invalid` e `aria-describedby` quando há erro
- `role="alert"` na mensagem de erro

**Dependências:** nenhuma.

---

## FEEDBACK

---

### `Button`

**Responsabilidade:** Elemento de acção primária, secundária, ghost e WhatsApp.

**Variações:**
- `primary` — laranja sólido
- `secondary` — outline carvão
- `ghost` — sem fundo
- `whatsapp` — verde WhatsApp
- `icon` — apenas ícone (com aria-label obrigatório)

**Tamanhos:**
- `sm` — 36px de altura
- `md` — 44px (default)
- `lg` — 52px (hero CTA)

**Propriedades essenciais:**
- `variant?: 'primary' | 'secondary' | 'ghost' | 'whatsapp' | 'icon'`
- `size?: 'sm' | 'md' | 'lg'`
- `disabled?: boolean`
- `loading?: boolean` — estado de carregamento com spinner
- `aria-label?: string` — obrigatório em variante `icon`
- `children: ReactNode`

**Server ou Client:** Server Component (pode ser renderizado no servidor); adicionar `"use client"` apenas se necessário para event handlers locais.

**Acessibilidade:**
- `disabled` + `aria-disabled="true"` quando desactivado
- `aria-busy="true"` quando `loading={true}`
- `aria-label` obrigatório em botões sem texto visível
- Foco visível em todos os estados

**Dependências:** nenhuma.

---

### `SectionLabel`

**Responsabilidade:** Label de secção em uppercase para identificar visualmente a área da página.

**Variações:**
- `primary` — texto laranja
- `muted` — texto muted-foreground

**Propriedades essenciais:**
- `children: ReactNode`
- `variant?: 'primary' | 'muted'`

**Server ou Client:** Server Component.

**Acessibilidade:** `<p>` ou `<span>`, nunca `<h>` (não é um heading). Texto uppercase é implementado via CSS, não transformação do texto no HTML (leitores de ecrã lêm o original).

**Dependências:** nenhuma.

---

### `EmptyState`

**Responsabilidade:** Estado de ausência de conteúdo. Usado quando serviços ou galeria não têm dados.

**Variações:**
- `services-empty`
- `gallery-empty`

**Propriedades essenciais:**
- `type: 'services' | 'gallery'`
- `whatsappNumber?: string`

**Server ou Client:** Server Component.

**Acessibilidade:** texto descritivo claro do estado. Nenhum placeholder visual genérico.

**Dependências:** `Button`

---

## DASHBOARD FUTURE (Painel Administrativo — Sprints Futuras)

Os seguintes componentes são antecipados para o painel admin mas **não implementados** na Sprint 7B.

| Componente | Responsabilidade |
|---|---|
| `DashboardLayout` | Layout do painel com sidebar, header de admin |
| `ServiceEditor` | CRUD de serviços com drag-and-drop para reordenar |
| `GalleryUploader` | Upload de imagens antes/depois com preview |
| `BrandingEditor` | Formulário de cores, logo, slogan |
| `SettingsForm` | Configurações da empresa (moeda, IVA, formato de data) |
| `CompanyProfileForm` | Edição dos dados da empresa |
| `BeforeAfterUploader` | Upload especializado com validação de par de imagens |
| `MetricsWidget` | Widget de estatística para o dashboard |
| `DataTable` | Tabela de dados com sort e paginação (admin) |
| `ConfirmDialog` | Dialog de confirmação de acção destrutiva |
| `ToastNotification` | Feedback toast de acções (save, delete, error) |

Estes componentes são especificados apenas para referência de planeamento. A sua implementação é condicionada às sprints de desenvolvimento do painel administrativo.
