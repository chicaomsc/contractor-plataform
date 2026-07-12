# Roadmap do Produto

**Versão:** 2.1 — Sprint 7A concluída (incl. refinamento)  
**Data:** 2026-07-12  
**Horizonte:** MVP + Pós-MVP imediato

---

## Princípios de Priorização

1. **Valor para o utilizador beta primeiro:** cada entrega deve resolver um problema concreto do prestador de serviço.
2. **Infraestrutura antes de funcionalidade:** segurança, isolamento de dados e deploy devem estar prontos antes de qualquer módulo de negócio.
3. **Sem features "por precaução":** nada entra no roadmap sem um caso de uso identificado.
4. **Entregas verticais:** cada sprint entrega uma fatia funcional completa, não uma camada horizontal.
5. **Design antes do código:** decisões visuais documentadas antes da implementação de frontend.

---

## Estado Actual

| Item | Status |
|---|---|
| Estrutura de monorepo (`/backend`, `/frontend`, `/docs`, `/docker`) | Concluído |
| Documentação arquitetural (ADRs, domínio, módulos) | Concluído |
| Backend — Autenticação (JWT, refresh, registro) | Concluído |
| Backend — Módulo Company (perfil, branding, settings, logo) | Concluído |
| Backend — Módulo Service Catalogue + Gallery | Concluído |
| Backend — API Pública (`/public/services`, `/public/gallery`) | Concluído |
| Design Direction — Landing JR Pinturas | Concluído |
| Frontend — Foundation (Next.js, tokens, layout, SEO) | Pendente (Sprint 7B) |
| Frontend — Landing Page Pública | Pendente (Sprint 7C) |
| Frontend — Painel Administrativo | Não iniciado |

---

## Sprint 1 — Fundação Arquitetural ✅

**Objectivo:** Zero código de produção. Toda a equipa alinhada na arquitectura antes de qualquer implementação.

**Artefactos:**
- [x] ADR-000: Visão e Escopo
- [x] ADR-001: Estilo de Arquitectura
- [x] Modelo de Domínio
- [x] Diagrama de Módulos
- [x] Roadmap inicial

**Status:** Concluída

---

## Sprint 2 — Infraestrutura e Autenticação ✅

**Objectivo:** Sistema rodando end-to-end com autenticação funcional.

**Backend:**
- [x] Projecto Spring Boot com Java 25
- [x] Configuração PostgreSQL + Flyway
- [x] Módulo `auth`: registo, login, JWT (HS256), refresh tokens (opacos UUID)
- [x] `JwtPrincipal` stateless (sem DB lookup por request)
- [x] `GlobalExceptionHandler` com ProblemDetail (RFC 9457)
- [x] Testcontainers (padrão static initializer + @DynamicPropertySource)

**Status:** Concluída

---

## Sprint 3 — Módulo Company ✅

**Objectivo:** Perfil de empresa, branding e configurações.

**Backend:**
- [x] `GET/PUT /company/me`, `GET/PUT /branding/me`, `GET/PUT /settings/me`
- [x] `POST/DELETE /company/logo`
- [x] `StorageService` (Strategy Pattern) com `LocalStorageService`
- [x] Apenas OWNER pode modificar

**Status:** Concluída

---

## Sprint 4 — Autenticação (continuação) ✅

> Nota: as sprints internas de desenvolvimento não seguiram exactamente a numeração do roadmap original. Sprints 2–4 foram executadas em sequência com foco no backend.

**Status:** Absorvida pelas sprints 2–3.

---

## Sprint 5 — Catálogo de Serviços e Galeria ✅

**Objectivo:** Gestão de serviços e galeria de portfólio.

**Backend:**
- [x] `Service` entity com slug único por empresa
- [x] `GalleryItem` entity com antes/depois
- [x] Admin APIs: `GET/POST/PUT/DELETE/PATCH /services`, `GET/POST/PUT/DELETE/PATCH /gallery`
- [x] Upload de imagens antes/depois
- [x] API Pública: `GET /public/services?slug=...`, `GET /public/gallery?slug=...`
- [x] `ServiceSlugGenerator` (slug único por empresa)
- [x] `GalleryImageService` (validação e armazenamento)
- [x] 146 testes — 0 falhas

**Status:** Concluída

---

## Sprint 7A — Design Direction ✅

**Objectivo:** Definir direcção visual, wireframes e design system antes de implementar o frontend.

**Artefactos produzidos:**
- [x] `docs/ui/visual-direction.md` — 3 direcções visuais, recomendação: "Obra em Ordem"
- [x] `docs/ui/content-structure.md` — estrutura de secções justificada
- [x] `docs/ui/landing-wireframe.md` — wireframes ASCII mobile, tablet, desktop
- [x] `docs/ui/design-system.md` — tokens semânticos, tipografia, espaçamento, componentes
- [x] `docs/ui/responsive-behavior.md` — estratégia mobile-first, breakpoints
- [x] `docs/ui/accessibility.md` — WCAG 2.2 AA, ARIA, semântica
- [x] `docs/ui/component-inventory.md` — inventário de componentes (sem código)
- [x] `docs/releases/v0.6.0-design.md` — documentação da release

**Refinamento (Fase 2):**
- [x] `docs/ui/content-guide.md` — guia de voz, pt-PT, regras de escrita
- [x] `docs/ui/motion.md` — tokens de animação, regras de movimento
- [x] `docs/ui/component-principles.md` — 12 princípios obrigatórios
- [x] `docs/ui/component-architecture.md` — hierarquia e naming de componentes
- [x] `docs/ui/design-system.md` v1.1 — Tokens First, novos tokens, sem HEX em specs
- [x] `docs/ui/visual-direction.md` — capítulo multi-tenant adicionado
- [x] `docs/ui/component-inventory.md` v1.1 — regras de naming genérico

**Direcção escolhida:** B — "Obra em Ordem"  
**Código produzido:** Nenhum.

**Critério de saída:** ✅ Todos os documentos consistentes e completos.

---

## Sprint 7B — Frontend Foundation (próxima)

**Objectivo:** Scaffolding técnico do frontend. Sem landing content — apenas a fundação reutilizável.

**Sem pré-requisitos de cliente.** Esta sprint é viável imediatamente.

**Frontend:**
- [ ] Setup Next.js 15 + TypeScript + Tailwind CSS
- [ ] Tokens CSS do design system (`lib/tokens.css`)
- [ ] `RootLayout`, `SiteLayout`
- [ ] `SiteHeader` + `NavDrawer` (mobile)
- [ ] `SiteFooter`
- [ ] `WhatsAppFAB`
- [ ] SEO base: metadata, Open Graph, Twitter Card
- [ ] `sitemap.xml` dinâmico por slug
- [ ] `robots.txt`
- [ ] Integração da API pública (fetch tipado, `CompanyPublicData`)
- [ ] Página 404 (`not-found.tsx`)

**Backend (adição necessária):**
- [ ] `GET /public/company?slug={slug}` — endpoint público de perfil da empresa

**Critério de saída:** Qualquer URL `/{slug}` carrega um site funcional com header, footer e page chrome correctos. Secções de conteúdo ainda não implementadas.

---

## Sprint 7C — Landing Page Pública

**Objectivo:** Implementar todas as secções de conteúdo da landing com dados reais.

**Pré-requisitos bloqueantes:**
- [ ] Sprint 7B concluída
- [ ] Logo do cliente em alta resolução recebida
- [ ] Mínimo 2 fotografias de obra para o hero
- [ ] Paleta final validada com a logo real
- [ ] Mínimo 4 pares antes/depois disponíveis

**Frontend:**
- [ ] `HeroSection`
- [ ] `TrustStrip` + `StatCounter`
- [ ] `ServiceSection` + fetch de `/public/services`
- [ ] `BeforeAfterSection` + `BeforeAfterComparison` + fetch de `/public/gallery`
- [ ] `ProcessSection`
- [ ] `AboutSection`
- [ ] `CoverageSection`
- [ ] `ContactSection` + `ContactForm`
- [ ] Responsividade mobile-first end-to-end
- [ ] Acessibilidade WCAG 2.2 AA (Lighthouse ≥ 90)
- [ ] Performance (LCP < 2.5s, WebP, lazy loading)

**Critério de saída:** Landing page acessível via `/{slug}` com dados reais. Passa Lighthouse Accessibility ≥ 90 e Performance ≥ 80.

---

## Sprint 8 — Painel Administrativo (frontend)

**Objectivo:** Interface de gestão da empresa, serviços e galeria.

**Frontend:**
- [ ] Autenticação (login, logout, refresh)
- [ ] Layout do painel (sidebar, header)
- [ ] Gestão de perfil da empresa
- [ ] Gestão de branding e logo
- [ ] Gestão de serviços (CRUD + reordenar)
- [ ] Gestão de galeria (CRUD + upload de imagens)
- [ ] Configurações da conta

**Critério de saída:** Admin consegue gerir o conteúdo da landing sem tocar em código.

---

## Sprint 9 — Clientes e Orçamentos

**Objectivo:** Gestão de carteira de clientes e criação de orçamentos.

**Backend:**
- [ ] Módulo `customer`: CRUD de `Customer`
- [ ] Módulo `estimate`: criação de `Estimate` com `EstimateItem`
- [ ] `referenceNumber` sequencial por empresa
- [ ] Cálculo de totais com IVA

**Frontend:**
- [ ] Página de gestão de clientes
- [ ] Formulário de criação/edição de orçamento
- [ ] Adição/remoção de itens

**Critério de saída:** Admin cria orçamento completo para um cliente.

---

## Sprint 10 — PDF e Materiais

**Objectivo:** Orçamentos com materiais, exportáveis em PDF.

**Backend:**
- [ ] `Material` associado a `EstimateItem`
- [ ] Geração de PDF com branding da empresa
- [ ] `POST /estimates/{id}/pdf`

**Frontend:**
- [ ] Gestão de materiais por item
- [ ] Preview e download do PDF

**Critério de saída:** PDF gerado com logo, cores, itens e totais correctos.

---

## Sprint 11 — Hardening e Deploy

**Objectivo:** Sistema estável, seguro e em produção.

**Backend:**
- [ ] Rate limiting no login
- [ ] Logs estruturados (JSON)
- [ ] Deploy no Railway com variáveis de produção

**Frontend:**
- [ ] Deploy no Vercel
- [ ] Tratamento de erros globais (404, error boundary)
- [ ] Loading states consistentes

**Infra:**
- [ ] PostgreSQL gerenciado
- [ ] Backup automático
- [ ] CORS configurado para domínio de produção

**Critério de saída:** Sistema em produção com o cliente beta. Fluxos do MVP testados manualmente.

---

## Pós-MVP — Backlog Prioritário

| Funcionalidade | Justificativa |
|---|---|
| Depoimentos na landing page | Prova social para conversão |
| Transições de status de orçamento com notificação por email | Comunicação com o cliente final |
| Dashboard com métricas (orçamentos por status, receita prevista) | Visibilidade para o prestador |
| Auto-cadastro (multi-tenant self-service) | Escalar além do cliente beta |
| CRM leve (histórico de interações por cliente) | Retenção e relacionamento |
| WhatsApp API (envio de orçamento via WhatsApp) | Canal preferencial em Portugal |
| Integração de pagamento (Stripe) | Monetização |
| Agenda/calendário de execução | Gestão operacional |
| App mobile (React Native ou PWA) | Mobilidade para o prestador em campo |

---

## Dependências Críticas (Riscos)

| Dependência | Risco | Mitigação |
|---|---|---|
| Fotografias reais da JR Pinturas | Sem fotos, o hero e galeria não funcionam | Documentado em `content-structure.md`; fallbacks especificados |
| Logo validado com paleta | Paleta proposta pode não funcionar com a logo real | Tokens marcados como "sujeitos a validação"; ajuste antes da Sprint 7B |
| Supabase Storage | Serviço externo fora do controlo | Interface de storage abstraída — troca por S3/R2 não quebra o domínio |
| Railway (backend) | Custo pode aumentar com uso | Dockerfile portável |
| Java 25 (LTS) | Versão recente, ecossistema em adaptação | Usar apenas features estáveis |
| Geração de PDF | Bibliotecas Java requerem avaliação de licença | Decidir na Sprint 10; Flying Saucer (LGPL) candidato principal |

---

## Referências

- [ADR-000 — Visão e Escopo](adr/ADR-000-vision-and-scope.md)
- [ADR-001 — Estilo de Arquitectura](adr/ADR-001-architecture-style.md)
- [Modelo de Domínio](architecture/domain-model.md)
- [Diagrama de Módulos](architecture/module-diagram.md)
- [Direcção Visual](ui/visual-direction.md)
- [Estrutura de Conteúdo](ui/content-structure.md)
- [Wireframes](ui/landing-wireframe.md)
- [Design System](ui/design-system.md)
- [Responsividade](ui/responsive-behavior.md)
- [Acessibilidade](ui/accessibility.md)
- [Inventário de Componentes](ui/component-inventory.md)
- [Guia de Conteúdo](ui/content-guide.md)
- [Motion](ui/motion.md)
- [Princípios de Componentes](ui/component-principles.md)
- [Arquitectura de Componentes](ui/component-architecture.md)
- [Release v0.6.0-design](releases/v0.6.0-design.md)
