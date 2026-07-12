# Arquitectura de Componentes — Contractor Platform

**Sprint:** 7A — Design Direction (refinamento)  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Aplicação:** Frontend (Next.js 15, TypeScript)

---

> Este documento define a hierarquia e responsabilidade de cada componente previsto. Nenhum código foi escrito — este é o mapa de implementação para a Sprint 7B e 7C.

---

## Estrutura de Pastas (Prevista)

```
frontend/
├── app/                          ← Next.js App Router
│   ├── [slug]/                   ← Landing page do tenant (rota pública)
│   │   ├── page.tsx
│   │   └── layout.tsx
│   ├── admin/                    ← Painel administrativo (protegido)
│   │   ├── layout.tsx
│   │   └── ...
│   ├── layout.tsx                ← Root layout (html, body, providers)
│   └── not-found.tsx
│
├── components/
│   ├── layout/                   ← Estrutura de página
│   ├── sections/                 ← Secções da landing
│   ├── marketing/                ← Componentes de marketing genéricos
│   ├── gallery/                  ← Galeria e comparador
│   ├── forms/                    ← Formulários
│   ├── ui/                       ← Componentes atómicos (Button, Input, etc.)
│   └── dashboard/                ← Componentes do painel admin (futuro)
│
├── lib/
│   ├── api/                      ← Funções de fetch para a API pública e admin
│   ├── tokens.css                ← Variáveis CSS do design system
│   └── utils.ts
│
└── public/
```

---

## Hierarquia de Renderização — Landing Page

```
app/[slug]/layout.tsx (Server)
│
└── SiteLayout (Server)
    ├── SiteHeader (Híbrido)
    │   └── NavDrawer (Client) ← abertura mobile
    │
    ├── main#main-content
    │   ├── HeroSection (Server)
    │   │   └── Button (Server)
    │   │
    │   ├── TrustStrip (Híbrido)
    │   │   └── StatCounter (Client) ← count-up animation
    │   │
    │   ├── ServiceSection (Server)
    │   │   └── ServiceItem (Server) × N
    │   │
    │   ├── BeforeAfterSection (Server)
    │   │   └── BeforeAfterComparison (Client) × N ← slider interactivo
    │   │
    │   ├── ProcessSection (Server)
    │   │   └── ProcessStep (Server) × 4
    │   │
    │   ├── AboutSection (Server)
    │   │
    │   ├── CoverageSection (Server)
    │   │
    │   └── ContactSection (Híbrido)
    │       └── ContactForm (Client) ← estado de formulário
    │
    ├── SiteFooter (Server)
    │
    └── WhatsAppFAB (Client) ← fixed position, pulse animation
```

---

## LAYOUTS

---

### `SiteLayout`

**Caminho:** `components/layout/SiteLayout.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Aplica variáveis CSS de branding do tenant (cores, fontes via `style` prop), define a estrutura semântica base (`html`, `body`, skip link). Passa dados da empresa para o header e footer.

**Recebe:**
- `company: CompanyPublicData` — fonte de verdade do branding
- `children: ReactNode`

**Não faz:**
- Fetch de dados (responsabilidade da `page.tsx`)
- Lógica de negócio

---

### `AdminLayout` *(Sprint 8)*

**Caminho:** `components/layout/AdminLayout.tsx`  
**Tipo:** Server Component (com Client filhos)  
**Responsabilidade:** Layout do painel administrativo. Sidebar de navegação, header de admin, área de conteúdo.

---

## SECTIONS

Cada Section é um componente de secção completa da landing. Recebe dados da API como props e renderiza o layout correspondente. Nenhuma Section conhece o tenant.

---

### `HeroSection`

**Caminho:** `components/sections/HeroSection.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Primeira secção visível. Promessa principal, imagem de obra, CTAs primário e secundário.

**Recebe:**
```typescript
{
  headline:       string
  subheadline:    string
  primaryCta:     { label: string; href: string }
  secondaryCta?:  { label: string; href: string }
  imageUrl?:      string
  imageAlt?:      string
}
```

**Variações:** `with-image` / `no-image` (fallback de textura quando `imageUrl` é nulo)

---

### `TrustStrip`

**Caminho:** `components/sections/TrustStrip.tsx`  
**Tipo:** Híbrido (Server + Client para count-up)  
**Responsabilidade:** Barra de métricas de credibilidade (anos, obras, clientes, avaliação). Fundo escuro (`--surface-dark`).

**Recebe:**
```typescript
{
  metrics: Array<{
    value:   string | number
    label:   string
    suffix?: string
  }>
}
```

---

### `ServiceSection`

**Caminho:** `components/sections/ServiceSection.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Lista de serviços activos do tenant. Renderiza empty state se a lista estiver vazia.

**Recebe:**
```typescript
{
  services: PublicServiceResponse[]
}
```

---

### `BeforeAfterSection`

**Caminho:** `components/sections/BeforeAfterSection.tsx`  
**Tipo:** Server Component (wrapper)  
**Responsabilidade:** Container da galeria de antes/depois. Oculta a secção inteira se não houver items com imagens.

**Recebe:**
```typescript
{
  items: PublicGalleryResponse[]
}
```

**Nota:** renderiza `null` se `items.length === 0` (secção ocultada — não exibe empty state na landing).

---

### `ProcessSection`

**Caminho:** `components/sections/ProcessSection.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** 4 passos do processo de trabalho. Conteúdo estático configurável via props (não via API).

**Recebe:**
```typescript
{
  steps: Array<{
    number:      number
    title:       string
    description: string
  }>
  cta?: { label: string; href: string }
}
```

---

### `AboutSection`

**Caminho:** `components/sections/AboutSection.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Secção "Sobre nós". Texto e fotografia da equipa/fundador.

**Recebe:**
```typescript
{
  content:   string       ← texto (pode ser HTML processado ou markdown)
  photoUrl?: string
  photoAlt?: string
}
```

---

### `CoverageSection`

**Caminho:** `components/sections/CoverageSection.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Área geográfica de actuação.

**Recebe:**
```typescript
{
  areas:              string[]
  outOfAreaMessage?:  string
  whatsappNumber:     string
}
```

---

### `ContactSection`

**Caminho:** `components/sections/ContactSection.tsx`  
**Tipo:** Híbrido (Server wrapper + Client form)  
**Responsabilidade:** CTA final com botão WhatsApp e formulário alternativo de contacto.

**Recebe:**
```typescript
{
  whatsappNumber:   string
  whatsappMessage?: string
  services:         Array<{ id: string; name: string }>
  email?:           string
}
```

---

## NAVIGATION

---

### `SiteHeader`

**Caminho:** `components/layout/SiteHeader.tsx`  
**Tipo:** Híbrido  
**Responsabilidade:** Header fixo com logo, links de navegação, botão WhatsApp. Comprime ao scroll.

---

### `SiteFooter`

**Caminho:** `components/layout/SiteFooter.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Logo, links legais, redes sociais, copyright.

---

### `NavDrawer`

**Caminho:** `components/layout/NavDrawer.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Menu lateral mobile. Trap de foco, fechar com ESC.

---

### `WhatsAppFAB`

**Caminho:** `components/layout/WhatsAppFAB.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Botão flutuante fixo de WhatsApp em mobile. `position: fixed`.

---

## GALLERY

---

### `BeforeAfterComparison`

**Caminho:** `components/gallery/BeforeAfterComparison.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Comparador interactivo (desktop: slider drag; mobile: imagens empilhadas). Suporte completo a teclado e ARIA.

---

### `GalleryCarousel`

**Caminho:** `components/gallery/GalleryCarousel.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Navegação entre pares de imagens da galeria. Dots de paginação e botões ← →.

---

## FORMS

---

### `ContactForm`

**Caminho:** `components/forms/ContactForm.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Formulário de pedido de orçamento. Validação, estado de loading, feedback de sucesso/erro.

---

### `FormField`

**Caminho:** `components/forms/FormField.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Campo atómico com label, input/textarea/select, mensagem de erro. Acessível com `aria-invalid`, `aria-describedby`.

---

## UI (Componentes Atómicos)

---

### `Button`

**Caminho:** `components/ui/Button.tsx`  
**Tipo:** Server Component (por defeito); Client se precisar de event handlers locais  
**Variantes:** `primary`, `secondary`, `ghost`, `whatsapp`, `icon`  
**Tamanhos:** `sm`, `md`, `lg`

---

### `SectionLabel`

**Caminho:** `components/ui/SectionLabel.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Label de secção em uppercase. Renderiza `<p>` ou `<span>`, nunca `<h>`.

---

### `EmptyState`

**Caminho:** `components/ui/EmptyState.tsx`  
**Tipo:** Server Component  
**Responsabilidade:** Estado de ausência de conteúdo com mensagem descritiva e CTA.

---

### `SkeletonBlock`

**Caminho:** `components/ui/SkeletonBlock.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Bloco de skeleton loading com animação shimmer.

---

### `StatCounter`

**Caminho:** `components/ui/StatCounter.tsx`  
**Tipo:** Client Component  
**Responsabilidade:** Número com count-up ao entrar no viewport. Desactivado com `prefers-reduced-motion`.

---

## DASHBOARD (Sprint 8 — não implementar antes)

Componentes previstos para o painel administrativo. **Não criar antes da Sprint 8.**

| Componente | Caminho | Responsabilidade |
|---|---|---|
| `DashboardSidebar` | `components/dashboard/DashboardSidebar.tsx` | Navegação lateral do painel |
| `ServiceEditor` | `components/dashboard/ServiceEditor.tsx` | CRUD + reordenação de serviços |
| `GalleryUploader` | `components/dashboard/GalleryUploader.tsx` | Upload de imagens antes/depois |
| `BrandingEditor` | `components/dashboard/BrandingEditor.tsx` | Cores, logo, slogan |
| `CompanyProfileForm` | `components/dashboard/CompanyProfileForm.tsx` | Dados da empresa |
| `SettingsForm` | `components/dashboard/SettingsForm.tsx` | Moeda, IVA, formatos |
| `ConfirmDialog` | `components/dashboard/ConfirmDialog.tsx` | Confirmação de acção destrutiva |
| `ToastNotification` | `components/ui/ToastNotification.tsx` | Feedback toast de acções |
| `DataTable` | `components/ui/DataTable.tsx` | Tabela com sort e paginação |

---

## Fluxo de Dados — Landing Page

```
app/[slug]/page.tsx
│
├── fetch('/public/company?slug={slug}')      → CompanyPublicData
├── fetch('/public/services?slug={slug}')     → PublicServiceResponse[]
└── fetch('/public/gallery?slug={slug}')      → PublicGalleryResponse[]
         │
         ▼
    SiteLayout (company)
         │
         ├── SiteHeader       ← logoUrl, name, whatsapp
         ├── HeroSection      ← headline, imageUrl (se existir no branding)
         ├── TrustStrip       ← metrics (estáticos ou config da empresa)
         ├── ServiceSection   ← services
         ├── BeforeAfterSection ← gallery items com imagens
         ├── ProcessSection   ← steps (estáticos, podem ser config)
         ├── AboutSection     ← description (company.description)
         ├── CoverageSection  ← areas (config da empresa)
         ├── ContactSection   ← whatsapp, email, services (para dropdown)
         └── SiteFooter       ← name, taxNumber, socialLinks
```

---

## Regras de Naming

| Categoria | Padrão | Exemplo |
|---|---|---|
| Layouts | `[Name]Layout` | `SiteLayout`, `AdminLayout` |
| Sections | `[Name]Section` | `HeroSection`, `ServiceSection` |
| Standalone marketing | `[Name]Strip` / `[Name]Block` | `TrustStrip` |
| Gallery | `[Name]Comparison` / `[Name]Carousel` | `BeforeAfterComparison` |
| Forms | `[Name]Form` / `[Name]Field` | `ContactForm`, `FormField` |
| UI atómicos | `[Name]` | `Button`, `EmptyState` |
| Dashboard | `[Entity]Editor` / `[Entity]Form` | `ServiceEditor`, `BrandingEditor` |

**Proibido:** prefixos de tenant (`JR`, `Pinturas`), sufixos de tecnologia (`Component`, `Widget`, `Container`), abreviaturas não óbvias.
