# Roadmap do Produto

**Versão:** 2.13 — Sprint 10A concluída  
**Data:** 2026-07-16  
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
| Frontend — Foundation (Next.js, tokens, layout, SEO) | Concluído |
| Frontend — Integração pública (hooks, DTOs, preview técnico) | Concluído |
| Spike — Impeccable no comparador antes/depois | Concluído (REJECT) |
| Frontend — Landing Page Pública | Concluído |
| Frontend — Landing Polish & Production Readiness | Concluído |
| Real Tenant Validation — JR Pinturas | Concluído |
| Production Visual QA — JR Pinturas | Concluído |
| Dashboard Foundation | Concluído |
| Services Management | Concluído |
| Gallery Management | Concluído |
| MVP Hardening | Concluído |
| Frontend — Painel Administrativo completo | Concluído |
| Backend — Módulo Customer + Estimate (domínio, cálculos, API) | Concluído |

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

## Sprint 7B — Frontend Foundation ✅

**Objectivo:** Scaffolding técnico do frontend. Sem landing content — apenas a fundação reutilizável.

**Sem pré-requisitos de cliente.** Esta sprint é viável imediatamente.

**Frontend:**
- [x] Setup Next.js App Router + TypeScript + Tailwind CSS
- [x] Tokens CSS do design system
- [x] `RootLayout` e `PublicLayout`
- [x] `SiteHeader`
- [x] `SiteFooter`
- [x] SEO base: metadata, Open Graph, viewport
- [x] `sitemap.xml`
- [x] `robots.txt`
- [x] Integração da API pública com cliente REST tipado
- [x] Página 404 (`not-found.tsx`)
- [x] Estados globais: loading, error, global error e fallback de API
- [x] Testes básicos e CI frontend

**Backend:**
- [x] `GET /public/sites/{companySlug}` — endpoint público de perfil da empresa
- [x] `GET /public/sites/{companySlug}/services`
- [x] `GET /public/sites/{companySlug}/gallery`

**Critério de saída:** `/` carrega header, placeholder técnico e footer com dados públicos por slug quando a API está disponível. Secções de conteúdo ainda não implementadas.

---

## Sprint 7B.5 — Public Site Integration ✅

**Objectivo:** Validar a integração frontend/backend dos contratos públicos antes da landing visual.

**Frontend:**
- [x] `usePublicSite`, `usePublicServices`, `usePublicGallery`
- [x] Query keys centralizadas
- [x] Separação API DTO → Mapper → ViewModel → Component
- [x] Preview técnico temporário em `/`
- [x] Loading, erro bloqueante, falha parcial e empty states
- [x] Branding seguro por CSS custom properties
- [x] Testes de hooks, mappers, API client e preview

**Backend:**
- [x] Contratos públicos validados por testes de integração
- [x] Isolamento por empresa confirmado
- [x] Slugs com acentos normalizados no onboarding
- [x] Before/after opcionais confirmados

**Critério de saída:** `/` prova o carregamento dos três endpoints públicos e mostra dados reais em formato técnico temporário, sem criar secções definitivas da landing.

---

## Spike 7B.1 — Avaliação controlada do Impeccable ✅

**Objectivo:** Avaliar o Impeccable em apenas um componente isolado antes da Sprint 7C.

**Frontend:**
- [x] Rota de laboratório `/_lab/before-after`
- [x] `BeforeAfterComparisonBaseline`
- [x] `BeforeAfterComparisonRefinedCandidate`
- [x] Fixtures locais sem imagens externas
- [x] Testes comportamentais para as duas versões
- [x] Rota não indexável e fora do sitemap

**Resultado:** REJECT.

**Justificativa:** O Impeccable não estava disponível como dependência, CLI, plugin ou connector executável neste ambiente. A adoção foi rejeitada por falta de avaliação reproduzível e auditável.

**Critério de saída:** Spike documentado em `docs/spikes/SPIKE-001-impeccable-before-after.md` e release `docs/releases/v0.7.2-spike.md`, sem iniciar a Sprint 7C.

---

## Sprint 7C — Landing Page Pública ✅

**Objectivo:** Implementar as secções públicas da landing com dados reais quando disponíveis, sem hardcode do tenant e com fallbacks profissionais para dados ausentes.

**Frontend:**
- [x] `HeroSection`
- [x] `TrustStrip`
- [x] `ServiceSection`
- [x] `GallerySection` com `BeforeAfterComparisonBaseline`
- [x] `ProcessSection`
- [x] `AboutSection`
- [x] `ServiceAreaSection`
- [x] `ContactSection` sem formulário fictício
- [x] `MobileWhatsAppAction`
- [x] Header/footer definitivos
- [x] Responsividade mobile-first
- [x] Acessibilidade WCAG 2.2 AA aplicada na implementação
- [x] SEO com dados públicos quando disponíveis
- [x] Testes comportamentais da landing

**Nota:** logo, fotografias e pares antes/depois reais deixaram de ser bloqueantes técnicos. Quando ausentes, a landing degrada com fallbacks neutros ou omite seções opcionais.

**Critério de saída:** `/` renderiza a landing pública final para o slug configurado, consumindo os três endpoints públicos, sem regra de negócio no Next.js e sem conteúdo hardcoded do tenant.

---

## Sprint 7D — Landing Polish & Production Readiness ✅

**Objectivo:** Refinar a landing existente para produção sem adicionar funcionalidades, endpoints, novas secções, ViewModels ou alterações no backend.

**Frontend:**
- [x] Revisão visual completa da landing existente
- [x] Ajustes de tipografia, espaçamento e ritmo editorial
- [x] Microinterações subtis em header, links, botões e CTAs
- [x] Contraste AA reforçado nos tokens e no branding público
- [x] Header, hero, serviços, galeria, processo, sobre, área, contacto e footer refinados
- [x] Validação responsiva em 320, 375, 768, 1024, 1440 e 1920 px
- [x] Lighthouse local em build de produção dentro das metas
- [x] Documentação de release em `docs/releases/v0.8.1-polish.md`

**Critério de saída:** Landing pronta para produção visual, com Lighthouse 100/100/100/100 no cenário local validado, gates frontend/backend verdes e sem novas funcionalidades.

---

## Sprint 7E — Real Tenant Validation (JR Pinturas) ✅

**Objectivo:** Validar a plataforma com um tenant real, sem criar landing específica e sem especializar componentes, ViewModels ou regras de frontend para JR Pinturas.

**Fonte de verdade:**
- [x] `assets/tenants/jr-pinturas/content.md`
- [x] `assets/tenants/jr-pinturas/logo/jr-logo.png`

**Execução:**
- [x] Conteúdo disponível inventariado
- [x] Lacunas documentadas: website, redes sociais, morada completa, fotografias e pares antes/depois
- [x] Company populada via backend
- [x] Branding populado via backend
- [x] Settings populado via backend
- [x] Services populados via backend
- [x] Gallery validada como vazia por ausência de fotografias reais
- [x] Logo real carregado pela landing pública
- [x] Responsividade validada em 375, 768 e 1440 px
- [x] Screenshots em `frontend/screenshots/jr-pinturas-*.png`

**Correções genéricas realizadas:**
- [x] `/uploads/**` público no backend para assets que o próprio storage retorna
- [x] Resource handler para servir uploads locais
- [x] CORS configurável para frontend público local
- [x] Normalização de URLs relativas de assets no mapper público do frontend
- [x] Ajuste responsivo genérico do hero em mobile

**Critério de saída:** JR Pinturas renderiza pela landing multi-tenant existente, com dados chegando pelos endpoints e ViewModels públicos, sem hardcode permanente do tenant.

---

## Sprint 7F — Production Visual QA ✅

**Objectivo:** Validar e refinar visualmente a landing em nível de produção usando os ativos reais do tenant JR Pinturas, sem criar funcionalidades, especializar componentes ou alterar backend, ViewModels, Design System ou arquitectura.

**Fonte de verdade:**
- [x] `assets/tenants/jr-pinturas/content.md`
- [x] `assets/tenants/jr-pinturas/logo/jr-logo.png`

**Execução:**
- [x] Branding validado: logo, cores, contraste, header e footer
- [x] Hero validado com fallback genérico por ausência de fotografia pública
- [x] Galeria validada como empty state por ausência de imagens reais e pares antes/depois
- [x] Ritmo visual, alinhamentos, espaçamentos, CTAs, foco, hover e navegação revisados
- [x] Responsividade validada em 320, 375, 390, 768, 1024, 1440 e 1920 px
- [x] Screenshots em `frontend/screenshots/production-qa/jr-pinturas-*.png`
- [x] Lighthouse local em build de produção: Performance 95, Accessibility 100, Best Practices 100, SEO 100
- [x] Release documentada em `docs/releases/v0.8.3-production-qa.md`

**Correção genérica realizada:**
- [x] O fallback de fotografia do hero deixa de ocupar a primeira posição no mobile quando não há imagem pública, preservando a hierarquia textual e mantendo o fallback visível sem regra específica para JR Pinturas.

**Critério de saída:** Landing validada visualmente para produção com tenant real, mantendo renderização multi-tenant por DTOs, mappers e ViewModels existentes.

---

## Sprint 8A — Dashboard Foundation ✅

**Objectivo:** Construir a fundação da área autenticada sem criar novas funcionalidades de negócio e sem alterar backend, endpoints, arquitectura ou autenticação existente.

**Frontend:**
- [x] Login frontend consumindo `POST /auth/login`
- [x] Proteção de `/dashboard` com middleware e guard client-side
- [x] Layout administrativo com sidebar, header, breadcrumb, user menu e logout
- [x] Responsividade desktop/mobile
- [x] Dashboard Home com empresa, branding, status, contagens e última atualização
- [x] Tela completa de edição de Company
- [x] Tela completa de edição de Branding com preview em tempo real
- [x] Tela completa de edição de Settings
- [x] Estados de loading, saving, success, error e retry
- [x] Testes de schemas/helpers administrativos
- [x] Screenshots em `frontend/screenshots/dashboard-foundation/`
- [x] Release documentada em `docs/releases/v0.9.0-dashboard-foundation.md`

**Endpoints consumidos:**
- [x] `POST /auth/login`
- [x] `GET /auth/me`
- [x] `GET/PUT /company/me`
- [x] `GET/PUT /branding/me`
- [x] `GET/PUT /settings/me`
- [x] `GET /services` apenas para contagem
- [x] `GET /gallery` apenas para contagem

**Critério de saída:** Área autenticada base disponível, com edição de Company, Branding e Settings por endpoints existentes, sem antecipar Services, Gallery, Customers, Estimates, Analytics, Agenda ou dashboards financeiros/operacionais.

---

## Sprint 8B — Services Management ✅

**Objectivo:** Construir a área administrativa completa para gerenciamento dos serviços oferecidos pela empresa, sem alterar backend, endpoints, arquitetura ou regras de negócio.

**Frontend:**
- [x] Rota `/dashboard/services`
- [x] Listagem de serviços
- [x] Criação de serviço
- [x] Edição de serviço
- [x] Exclusão com confirmação
- [x] Ativação e desativação
- [x] Ordenação por mover para cima e mover para baixo
- [x] Estados de loading, saving, success, error, retry e empty
- [x] Componentes reutilizáveis para formulário, lista, card, status, estado vazio e diálogo de exclusão
- [x] Testes de schema e ordenação
- [x] Screenshots em `frontend/screenshots/services-management/`
- [x] Release documentada em `docs/releases/v0.9.1-services-management.md`

**Endpoints consumidos:**
- [x] `GET /services`
- [x] `POST /services`
- [x] `PUT /services/{id}`
- [x] `DELETE /services/{id}`
- [x] `PATCH /services/{id}/reorder`

**Critério de saída:** Admin consegue gerir serviços por endpoints existentes, e a landing pública reflete os serviços ativos sem código específico de tenant e sem implementar Gallery, Customers, Estimates, Analytics, Agenda, uploads ou dashboards financeiros/operacionais.

---

## Sprint 8C — Gallery Management ✅

**Objectivo:** Construir um Media Manager simples, consistente e reutilizável para administrar as imagens utilizadas na landing pública, sem alterar backend, endpoints, arquitetura ou regras de negócio.

**Frontend:**
- [x] Rota `/dashboard/gallery`
- [x] Listagem de itens da galeria
- [x] Criação de item
- [x] Upload de imagens before e after
- [x] Preview de pares before/after
- [x] Exclusão com confirmação
- [x] Edição de metadados
- [x] Ativação e desativação
- [x] Definir e remover destaque
- [x] Ordenação por mover para cima e mover para baixo
- [x] Validação local de formato e tamanho máximo de arquivo
- [x] Estados de loading, saving, uploading, success, error, retry e empty
- [x] Componentes reutilizáveis para grid, card, formulário, upload, preview, badges, estado vazio e diálogo de exclusão
- [x] Testes de schema, ordenação e validação de arquivo
- [x] Screenshots em `frontend/screenshots/gallery-management/`
- [x] Release documentada em `docs/releases/v0.9.2-gallery-management.md`

**Endpoints consumidos:**
- [x] `GET /gallery`
- [x] `POST /gallery`
- [x] `PUT /gallery/{id}`
- [x] `DELETE /gallery/{id}`
- [x] `PATCH /gallery/{id}/feature`
- [x] `PATCH /gallery/{id}/reorder`
- [x] `POST /gallery/{id}/before-image`
- [x] `POST /gallery/{id}/after-image`
- [x] `DELETE /gallery/{id}/before-image`
- [x] `DELETE /gallery/{id}/after-image`

**Critério de saída:** Admin consegue gerir a galeria por endpoints existentes, e a landing pública reflete imagens ativas e destacadas sem código específico de tenant e sem implementar crop, editor de imagem, compressão, filtros, clientes, orçamentos, analytics ou agenda.

---

## Sprint 8D — Painel Administrativo (frontend)

**Objectivo:** Interface de gestão da empresa, serviços e galeria.

**Frontend:**
- [x] Autenticação básica (login, logout, sessão)
- [x] Layout do painel (sidebar, header)
- [x] Gestão de perfil da empresa
- [x] Gestão de branding
- [ ] Gestão de logo
- [x] Gestão de serviços (CRUD + reordenar)
- [x] Gestão de galeria (CRUD + upload de imagens)
- [x] Configurações da conta

**Critério de saída:** Admin consegue gerir o conteúdo da landing sem tocar em código.

---

## Sprint 9 — MVP Hardening ✅

**Objectivo:** estabilizar o MVP completo antes do Estimate Builder.

**Entregue:**
- [x] Playwright E2E com fluxo principal e smoke tests
- [x] Revisão de autenticação e sessão
- [x] Testes de isolamento cross-tenant no backend
- [x] Política explícita de upload PNG/JPEG/WebP até 5 MB
- [x] Segurança de storage contra path traversal
- [x] Security headers em Next.js e Spring Boot
- [x] Build sem dependência obrigatória de Google Fonts
- [x] Correção responsiva do logout no drawer mobile
- [x] CI com smoke E2E e build backend
- [x] Documentação em `docs/security/*`

**Status:** Concluída

---

## Sprint 10A — Estimate Domain & API ✅

**Objectivo:** Domínio e API REST de orçamentos — a única fonte de verdade para cálculos financeiros passa a ser o backend. Sem frontend, sem PDF nesta etapa.

**Backend:**
- [x] Módulo `customer`: CRUD de `Customer` (exclusão é soft-delete — `active = false`)
- [x] Módulo `estimate`: `Estimate`, `EstimateItem`, `Material` (Material pertence diretamente ao Estimate, não ao EstimateItem)
- [x] `number` sequencial por empresa, gerado atomicamente (`INSERT ... ON CONFLICT DO UPDATE ... RETURNING`) — ver [ADR-007](adr/ADR-007-estimate-numbering-strategy.md)
- [x] Cálculo de totais com IVA e adiantamento (`EstimateCalculationService`, domínio puro, `BigDecimal` scale 2 `HALF_UP`)
- [x] Snapshots de `currency`/`vatRate`/`upfrontPercentage` no `Estimate` — alterar `Settings` depois não afeta orçamentos existentes
- [x] Máquina de estados de `EstimateStatus` com transições validadas (`EstimateStatusTransitionService`)
- [x] `Settings.upfrontPercentage` (migration V6) — snapshot default para novos orçamentos
- [x] Migrations V6–V8, isolamento multi-tenant testado (cross-tenant customer/estimate/service)
- [x] `PATCH /estimates/{id}/status`, `GET /estimates` com filtros `status`/`customerId`
- [x] OpenAPI atualizado, 265 testes (unitários + integração) verdes

**Fora do escopo desta etapa (ver Sprint 11):**
- [ ] Página de gestão de clientes (frontend)
- [ ] Formulário de criação/edição de orçamento (frontend)
- [ ] Geração de PDF

**Critério de saída:** Admin consegue criar, listar, editar e mudar o status de um orçamento completo via API, com isolamento multi-tenant e cálculos garantidos pelo backend. ✅ Atingido — ver [Release v1.0.0](releases/v1.0.0-estimate-domain-api.md).

**Status:** Concluída (backend). Frontend de orçamento adiado para a Sprint 11 junto com PDF.

---

## Sprint 11 — Frontend de Orçamentos, PDF e Materiais

**Objectivo:** Painel administrativo para clientes e orçamentos (consumindo a API da Sprint 10A), exportação de orçamentos em PDF.

**Backend:**
- [ ] Geração de PDF com branding da empresa
- [ ] `POST /estimates/{id}/pdf`

**Frontend:**
- [ ] Página de gestão de clientes
- [ ] Formulário de criação/edição de orçamento (itens e materiais — `Material` já pertence diretamente ao `Estimate`, ver [ADR/domain-model](architecture/domain-model.md))
- [ ] Preview e download do PDF

**Critério de saída:** Admin cria orçamento completo para um cliente pelo painel e exporta em PDF com logo, cores, itens e totais correctos.

---

## Sprint 12 — Deploy e Operação

**Objectivo:** Sistema em produção com operação mínima.

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
| Fotografias reais por tenant | Sem fotos, o hero e a galeria perdem força visual | Landing 7C usa fallbacks e omite seções opcionais quando necessário |
| Logo validado com paleta | Paleta pública pode não funcionar com a logo real | Branding passa por validação e cai para tokens seguros |
| Supabase Storage | Serviço externo fora do controlo | Interface de storage abstraída — troca por S3/R2 não quebra o domínio |
| Railway (backend) | Custo pode aumentar com uso | Dockerfile portável |
| Java 25 (LTS) | Versão recente, ecossistema em adaptação | Usar apenas features estáveis |
| Geração de PDF | Bibliotecas Java requerem avaliação de licença | Decidir na Sprint 11; Flying Saucer (LGPL) candidato principal |

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
- [ADR-006 — Estratégia de Renderização do Frontend](adr/ADR-006-frontend-rendering-strategy.md)
- [Release v0.7.0](releases/v0.7.0.md)
- [Release v0.7.1](releases/v0.7.1.md)
- [Spike 001 — Impeccable antes/depois](spikes/SPIKE-001-impeccable-before-after.md)
- [Release v0.7.2-spike](releases/v0.7.2-spike.md)
- [Release v0.8.3-production-qa](releases/v0.8.3-production-qa.md)
- [Release v0.9.0-dashboard-foundation](releases/v0.9.0-dashboard-foundation.md)
- [Release v0.9.1-services-management](releases/v0.9.1-services-management.md)
- [Release v0.9.2-gallery-management](releases/v0.9.2-gallery-management.md)
- [Release v0.9.3-mvp-hardening](releases/v0.9.3-mvp-hardening.md)
- [Security — Authentication Review](security/authentication-review.md)
- [Security — Tenant Isolation](security/tenant-isolation.md)
- [Security — Upload Policy](security/upload-policy.md)
- [Security — Dependency Audit](security/dependency-audit.md)
- [ADR-007 — Estratégia de Numeração de Orçamentos](adr/ADR-007-estimate-numbering-strategy.md)
- [Release v1.0.0 — Estimate Domain & API](releases/v1.0.0-estimate-domain-api.md)
